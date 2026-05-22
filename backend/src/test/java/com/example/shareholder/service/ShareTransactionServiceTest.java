package com.example.shareholder.service;

import com.example.shareholder.model.Person;
import com.example.shareholder.model.SharePrice;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.repository.PersonRepository;
import com.example.shareholder.repository.SharePriceRepository;
import com.example.shareholder.repository.ShareTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShareTransactionService covering the complete share sale workflow:
 * validation, price stamping, status defaulting, and search functionality.
 */
@ExtendWith(MockitoExtension.class)
class ShareTransactionServiceTest {

    @Mock private ShareTransactionRepository shareTransactionRepository;
    @Mock private PersonRepository personRepository;
    @Mock private SharePriceRepository sharePriceRepository;
    @Mock private ShareOwnershipService shareOwnershipService;
    @Mock private OwnerPercentageCalculator ownerPercentageCalculator;

    private ShareTransactionService service;

    @BeforeEach
    void setUp() {
        service = new ShareTransactionService(shareTransactionRepository, personRepository,
                sharePriceRepository, shareOwnershipService, ownerPercentageCalculator);
    }

    private Person person(long id, int shares) {
        return new Person("F", "L", "e@e.fi", "040", "Addr", "00100",
                "City", "010101-000" + id, shares, BigDecimal.ZERO, "FI00") {
            @Override public Long getId() { return id; }
        };
    }

    private ShareTransaction baseTransaction(Person seller, Person buyer, int shares) {
        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(shares);
        tx.setCollectionDate(LocalDate.now());
        tx.setTerm(LocalDate.now().plusDays(30));
        return tx;
    }

    // ── addShareTransaction – buyer/seller not found ─────────────────────────

    @Test
    void should_ThrowIllegalArgumentException_When_BuyerDoesNotExist() {
        // Domain: transaction cannot proceed without a valid buyer on record
        Person seller = person(1L, 200);
        Person buyer  = person(99L, 0);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        ShareTransaction tx = baseTransaction(seller, buyer, 10);
        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ostajaa ei löydy");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SellerDoesNotExist() {
        // Domain: transaction cannot proceed without a valid seller on record
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        ShareTransaction tx = baseTransaction(seller, buyer, 10);
        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Myyjää ei löydy");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_BuyerAndSellerAreTheSamePerson() {
        // Domain: self-sale is not a valid share transfer operation
        Person p = person(1L, 200);
        when(personRepository.findById(1L)).thenReturn(Optional.of(p));

        ShareTransaction tx = baseTransaction(p, p, 10);
        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sama henkilö");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SellerHasInsufficientShares() {
        // Domain: a shareholder cannot sell more shares than they hold
        Person seller = person(1L, 5);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareTransaction tx = baseTransaction(seller, buyer, 100);
        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tarpeeksi osakkeita");
    }

    // ── addShareTransaction – status validation ──────────────────────────────

    @ParameterizedTest(name = "invalid status ''{0}'' must be rejected")
    @ValueSource(strings = {"APPROVED", "unknown", "cancelled", "PENDING"})
    void should_ThrowIllegalArgumentException_When_StatusIsInvalid(String invalidStatus) {
        // Domain: transaction status must be one of the three allowed lifecycle states
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        ShareTransaction tx = baseTransaction(seller, buyer, 10);
        tx.setStatus(invalidStatus);
        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_DefaultStatusToPending_When_NoStatusProvided() {
        // Domain: unclassified transactions start in 'pending' review state
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(shareTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = baseTransaction(seller, buyer, 10);
        // status is null → must default to "pending"
        ShareTransaction result = service.addShareTransaction(tx);
        assertThat(result.getStatus()).isEqualTo("pending");
    }

    // ── searchShareTransactions ──────────────────────────────────────────────

    @Test
    void should_ReturnAllTransactions_When_SearchTermIsEmpty() {
        // Domain: empty search must show the full transaction history
        when(shareTransactionRepository.findAll()).thenReturn(List.of(new ShareTransaction()));
        List<ShareTransaction> result = service.searchShareTransactions("");
        assertThat(result).hasSize(1);
    }

    @Test
    void should_ReturnAllTransactions_When_SearchTermIsNull() {
        // Domain: null search term (missing query param) must behave the same as empty
        when(shareTransactionRepository.findAll()).thenReturn(List.of(new ShareTransaction()));
        List<ShareTransaction> result = service.searchShareTransactions(null);
        assertThat(result).hasSize(1);
    }

    // ── getTransactionsByStatus ──────────────────────────────────────────────

    @Test
    void should_ReturnEmptyList_When_StatusIsNullOrEmpty() {
        // Domain: status filter with no value must return empty rather than all records
        assertThat(service.getTransactionsByStatus(null)).isEmpty();
        assertThat(service.getTransactionsByStatus("")).isEmpty();
    }

    @Test
    void should_ReturnTransactionsByStatus_When_ValidStatusProvided() {
        // Domain: status-filtered list drives the approvals dashboard
        ShareTransaction tx = new ShareTransaction();
        when(shareTransactionRepository.findByStatus("approved")).thenReturn(List.of(tx));
        assertThat(service.getTransactionsByStatus("approved")).hasSize(1);
    }

    // ── getShareTransactionById ───────────────────────────────────────────────

    @Test
    void should_ReturnTransaction_When_IdExists() {
        // Domain: detail view requires lookup by primary key
        ShareTransaction tx = new ShareTransaction();
        when(shareTransactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        assertThat(service.getShareTransactionById(1L)).isSameAs(tx);
    }

    @Test
    void should_ThrowIllegalArgumentException_When_TransactionIdDoesNotExist() {
        // Domain: unknown transaction id must surface a clear error
        when(shareTransactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getShareTransactionById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_CollectionDateIsNull() {
        // Domain: mandatory fields must be present before persisting a transaction
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(10);
        tx.setCollectionDate(null);  // missing required date
        tx.setTerm(LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pakollisia");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_NumberOfSharesIsNegative() {
        // Domain: negative share count is never a valid transfer quantity
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareTransaction tx = baseTransaction(seller, buyer, -5);

        assertThatThrownBy(() -> service.addShareTransaction(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nolla tai negatiivinen");
    }

    // ── addShareTransaction – happy path ──────────────────────────────────────

    @Test
    void should_SaveTransactionAndUpdateOwnership_When_AllConditionsAreValid() {
        // Domain: approved transfer must persist the tx and refresh ownership records
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(sharePriceRepository.findFirstByOrderByIdDesc())
                .thenReturn(Optional.of(new SharePrice(new BigDecimal("10.00"))));
        when(shareTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = baseTransaction(seller, buyer, 50);
        tx.setStatus("approved");
        ShareTransaction result = service.addShareTransaction(tx);

        assertThat(result.getStatus()).isEqualTo("approved");
        assertThat(result.getPricePerShare()).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(shareOwnershipService).updateShareOwnership(any());
        verify(ownerPercentageCalculator).updateAllOwnershipPercentages();
    }

    @Test
    void should_SetPriceToZero_When_NoSharePriceExists() {
        // Domain: if no price is set yet, transaction defaults to zero price
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(shareTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = baseTransaction(seller, buyer, 10);
        ShareTransaction result = service.addShareTransaction(tx);

        assertThat(result.getPricePerShare()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── deleteShareTransaction ────────────────────────────────────────────────

    @Test
    void should_RestoreSharesToSeller_When_TransactionIsDeleted() {
        // Domain: rollback of a share sale must return shares to original owner
        Person seller = person(1L, 50);
        Person buyer  = person(2L, 100);
        ShareTransaction tx = baseTransaction(seller, buyer, 50);
        when(shareTransactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        service.deleteShareTransaction(1L);

        assertThat(seller.getNumberOfShares()).isEqualTo(100); // 50 + 50 returned
        assertThat(buyer.getNumberOfShares()).isEqualTo(50);   // 100 - 50 taken back
        verify(shareTransactionRepository).deleteById(1L);
    }

    @Test
    void should_ThrowRuntimeException_When_DeletingNonExistentTransaction() {
        // Domain: delete of unknown transaction must fail with a clear error
        when(shareTransactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteShareTransaction(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("transaktiota");
    }

    // ── updateShareTransaction ────────────────────────────────────────────────

    @Test
    void should_UpdateTransaction_When_ValidStatusProvided() {
        // Domain: status updates (e.g., pending → approved) must persist correctly
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        ShareTransaction existing = baseTransaction(seller, buyer, 10);
        when(shareTransactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(shareTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction updates = baseTransaction(seller, buyer, 10);
        updates.setStatus("approved");
        ShareTransaction result = service.updateShareTransaction(1L, updates);

        assertThat(result.getStatus()).isEqualTo("approved");
    }

    @Test
    void should_SetLatestPriceOnUpdate_When_SharePriceExists() {
        // Domain: update must stamp the current share price when one is available
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        ShareTransaction existing = baseTransaction(seller, buyer, 10);
        when(shareTransactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sharePriceRepository.findFirstByOrderByIdDesc())
                .thenReturn(Optional.of(new SharePrice(new BigDecimal("15.00"))));
        when(shareTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction updates = baseTransaction(seller, buyer, 10);
        updates.setStatus("pending");
        ShareTransaction result = service.updateShareTransaction(1L, updates);

        assertThat(result.getPricePerShare()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UpdateWithInvalidStatus() {
        // Domain: invalid status on update must be rejected
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        ShareTransaction existing = baseTransaction(seller, buyer, 10);
        when(shareTransactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        ShareTransaction updates = baseTransaction(seller, buyer, 10);
        updates.setStatus("INVALID");
        assertThatThrownBy(() -> service.updateShareTransaction(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status on pakollinen");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UpdatingNonExistentTransaction() {
        // Domain: update of unknown transaction must fail
        when(shareTransactionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateShareTransaction(999L, new ShareTransaction()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    // ── getShareTransactionsApproved ──────────────────────────────────────────

    @Test
    void should_ReturnApprovedTransactions_When_Called() {
        // Domain: report view shows only approved/completed share transfers
        when(shareTransactionRepository.findByStatus("approved")).thenReturn(List.of(new ShareTransaction()));
        assertThat(service.getShareTransactionsApproved()).hasSize(1);
    }

    // ── searchShareTransactions ───────────────────────────────────────────────

    @Test
    void should_SearchBySeller_When_NonEmptySearchTermProvided() {
        // Domain: transaction history can be filtered by seller name
        when(shareTransactionRepository
                .findBySellerFirstnameContainingIgnoreCaseOrSellerLastnameContainingIgnoreCase("Matti", "Matti"))
                .thenReturn(List.of(new ShareTransaction()));
        assertThat(service.searchShareTransactions("Matti")).hasSize(1);
    }

    // ── ShareTransaction model constructor ──────────────────────────────────

    @Test
    void should_SetStatusToPending_When_FullConstructorIsUsed() {
        // Domain: newly submitted transactions default to 'pending' awaiting approval
        Person seller = person(1L, 200);
        Person buyer  = person(2L, 0);
        ShareTransaction tx = new ShareTransaction(
                LocalDate.now(), LocalDate.now().plusDays(30),
                seller, buyer, false, 50,
                new BigDecimal("10.00"), new BigDecimal("500.00"), "notes");

        assertThat(tx.getStatus()).isEqualTo("pending");
        assertThat(tx.getNumberOfShares()).isEqualTo(50);
        assertThat(tx.getNotes()).isEqualTo("notes");
        assertThat(tx.isTransferTaxPaid()).isFalse();
    }
}

