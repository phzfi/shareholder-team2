package com.example.shareholder.service;

import com.example.shareholder.model.Person;
import com.example.shareholder.model.ShareOwnership;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.repository.PersonRepository;
import com.example.shareholder.repository.ShareOwnershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShareOwnershipService covering share ownership creation and transfer logic.
 */
@ExtendWith(MockitoExtension.class)
class ShareOwnershipServiceTest {

    @Mock private ShareOwnershipRepository shareOwnershipRepository;
    @Mock private PersonRepository personRepository;

    private ShareOwnershipService service;

    @BeforeEach
    void setUp() {
        service = new ShareOwnershipService(shareOwnershipRepository, personRepository);
    }

    private Person person(long id, int shares) {
        Person p = new Person("F", "L", "e@e.fi", "040", "Addr", "00100",
                "City", "010101-000" + id, shares, BigDecimal.ZERO, "FI00");
        // Reflectively set id for test purposes via a helper approach using the getter contract
        // We use a Spy-free approach: create a wrapper subclass inline is not needed since we
        // just need getters. Use a simple subclass trick via anonymous class.
        return new Person("F", "L", "e@e.fi", "040", "Addr", "00100",
                "City", "010101-000" + id, shares, BigDecimal.ZERO, "FI00") {
            @Override public Long getId() { return id; }
        };
    }

    // ── addShareOwnership ────────────────────────────────────────────────────

    @Test
    void should_CreateOwnershipRecord_When_PersonHasPositiveShareCount() {
        // Domain: issuing shares to a person must generate an ownership certificate
        Person p = person(1L, 100);
        when(shareOwnershipRepository.findMaxEndingShareNumber()).thenReturn(Optional.of(0));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareOwnership result = service.addShareOwnership(p);

        assertThat(result).isNotNull();
        assertThat(result.getNumberOfShares()).isEqualTo(100);
        assertThat(result.getStartingShareNumber()).isEqualTo(1);
        assertThat(result.getEndingShareNumber()).isEqualTo(100);
    }

    @Test
    void should_ReturnNull_When_PersonHasZeroShares() {
        // Domain: persons with no shares should not receive an ownership record
        Person p = person(1L, 0);
        ShareOwnership result = service.addShareOwnership(p);
        assertThat(result).isNull();
        verify(shareOwnershipRepository, never()).save(any());
    }

    @Test
    void should_AppendShareNumbers_When_ExistingOwnershipAlreadyPresent() {
        // Domain: share numbers must be sequential across all ownership records
        Person p = person(2L, 50);
        when(shareOwnershipRepository.findMaxEndingShareNumber()).thenReturn(Optional.of(200));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareOwnership result = service.addShareOwnership(p);

        assertThat(result.getStartingShareNumber()).isEqualTo(201);
        assertThat(result.getEndingShareNumber()).isEqualTo(250);
    }

    // ── updateShareOwnership ─────────────────────────────────────────────────

    @Test
    void should_ThrowIllegalArgumentException_When_SellerHasNoOwnerships() {
        // Domain: a person cannot sell shares they do not own
        Person seller = person(1L, 100);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(shareOwnershipRepository.findByOwnerId(1L)).thenReturn(List.of());

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(10);

        assertThatThrownBy(() -> service.updateShareOwnership(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Myyjällä ei ole osakkeita");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SellerHasInsufficientShares() {
        // Domain: a shareholder cannot transfer more shares than they hold
        Person seller = person(1L, 50);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareOwnership ownership = new ShareOwnership(50, 1, 50, seller);
        when(shareOwnershipRepository.findByOwnerId(1L)).thenReturn(List.of(ownership));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(100); // more than seller holds

        assertThatThrownBy(() -> service.updateShareOwnership(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tarpeeksi osakkeita");
    }

    @Test
    void should_TransferExactOwnershipBlock_When_SellerHasExactShares() {
        // Domain: full ownership block must move from seller to buyer when counts match exactly
        Person seller = person(1L, 50);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareOwnership ownership = new ShareOwnership(50, 1, 50, seller);
        when(shareOwnershipRepository.findByOwnerId(1L)).thenReturn(new ArrayList<>(List.of(ownership)));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(50);

        service.updateShareOwnership(tx);

        // ownership block should now belong to buyer
        assertThat(ownership.getOwner()).isSameAs(buyer);
        assertThat(seller.getNumberOfShares()).isEqualTo(0);
        assertThat(buyer.getNumberOfShares()).isEqualTo(50);
    }

    @Test
    void should_SplitOwnershipBlock_When_SellerHasMoreSharesThanSold() {
        // Domain: partial sale splits the ownership block - buyer gets sold portion, seller retains remainder
        Person seller = person(1L, 100);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        ShareOwnership ownership = new ShareOwnership(100, 1, 100, seller);
        when(shareOwnershipRepository.findByOwnerId(1L)).thenReturn(new ArrayList<>(List.of(ownership)));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(30);

        service.updateShareOwnership(tx);

        // seller retains 70 shares, buyer gets 30
        assertThat(seller.getNumberOfShares()).isEqualTo(70);
        assertThat(buyer.getNumberOfShares()).isEqualTo(30);
        // original ownership shrunk
        assertThat(ownership.getNumberOfShares()).isEqualTo(70);
    }

    @Test
    void should_ThrowIllegalArgumentException_When_BuyerDoesNotExist() {
        // Domain: transfer to non-existent buyer must be rejected
        Person seller = person(1L, 100);
        Person buyer  = person(99L, 0);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(10);

        assertThatThrownBy(() -> service.updateShareOwnership(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ostajaa ei löydy");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SellerDoesNotExist() {
        // Domain: transfer from non-existent seller must be rejected
        Person seller = person(1L, 100);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(10);

        assertThatThrownBy(() -> service.updateShareOwnership(tx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Myyjää ei löydy");
    }

    @Test
    void should_ReturnNull_When_TransactionHasZeroShares() {
        // Domain: zero-share transaction is a no-op and must not modify ownership records
        Person seller = person(1L, 100);
        Person buyer  = person(2L, 0);

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(0);

        ShareOwnership result = service.updateShareOwnership(tx);
        assertThat(result).isNull();
        verify(shareOwnershipRepository, never()).save(any());
    }

    @Test
    void should_GetAllShareOwnerships_When_Called() {
        // Domain: list of all ownership records must be fully retrievable for reporting
        when(shareOwnershipRepository.findAll()).thenReturn(List.of(new ShareOwnership()));
        assertThat(service.getAllShareOwnerships()).hasSize(1);
    }

    @Test
    void should_ReturnTotalCount_When_CountingShareOwnerships() {
        // Domain: dashboard metric for number of distinct ownership blocks
        when(shareOwnershipRepository.count()).thenReturn(5L);
        assertThat(service.getTotalShareOwnership()).isEqualTo(5L);
    }

    @Test
    void should_ReturnOwnershipById_When_IdExists() {
        // Domain: single ownership record lookup by primary key
        ShareOwnership so = new ShareOwnership(100, 1, 100, null);
        when(shareOwnershipRepository.findById(1L)).thenReturn(Optional.of(so));
        assertThat(service.getShareOwnershipById(1L)).isSameAs(so);
    }

    @Test
    void should_ReturnNull_When_OwnershipByIdDoesNotExist() {
        // Domain: missing ownership id returns null instead of throwing
        when(shareOwnershipRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.getShareOwnershipById(99L)).isNull();
    }

    @Test
    void should_TransferMultipleBlocks_When_SellerHasSeveralSmallerOwnerships() {
        // Domain: when one block is insufficient the service must consume multiple blocks in order
        Person seller = person(1L, 90);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        // Two blocks: 40 and 50 — sorted ascending → [40, 50]. To sell 70 we take 40 then 30 of 50.
        ShareOwnership block40 = new ShareOwnership(40, 1, 40, seller);
        ShareOwnership block50 = new ShareOwnership(50, 41, 90, seller);
        when(shareOwnershipRepository.findByOwnerId(1L))
                .thenReturn(new ArrayList<>(List.of(block40, block50)));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(70);

        service.updateShareOwnership(tx);

        // block40 fully transferred to buyer, block50 split
        assertThat(block40.getOwner()).isSameAs(buyer);
        assertThat(seller.getNumberOfShares()).isEqualTo(20);
        assertThat(buyer.getNumberOfShares()).isEqualTo(70);
    }

    @Test
    void should_BreakEarly_When_SaleIsFullfilled_BeforeAllBlocksIterated() {
        // Domain: loop must break as soon as remaining shares reach zero, skipping extra blocks
        Person seller = person(1L, 120);
        Person buyer  = person(2L, 0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(personRepository.findById(2L)).thenReturn(Optional.of(buyer));

        // Three blocks [30, 40, 50]. Sell 70: take 30+40=70 → remaining=0 → break before visiting block50
        ShareOwnership block30 = new ShareOwnership(30, 1, 30, seller);
        ShareOwnership block40 = new ShareOwnership(40, 31, 70, seller);
        ShareOwnership block50 = new ShareOwnership(50, 71, 120, seller);
        when(shareOwnershipRepository.findByOwnerId(1L))
                .thenReturn(new ArrayList<>(List.of(block30, block40, block50)));
        when(shareOwnershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareTransaction tx = new ShareTransaction();
        tx.setSeller(seller);
        tx.setBuyer(buyer);
        tx.setNumberOfShares(70);

        service.updateShareOwnership(tx);

        // block30 and block40 transferred; block50 untouched
        assertThat(block30.getOwner()).isSameAs(buyer);
        assertThat(block40.getOwner()).isSameAs(buyer);
        assertThat(block50.getOwner()).isSameAs(seller);
        assertThat(seller.getNumberOfShares()).isEqualTo(50);
        assertThat(buyer.getNumberOfShares()).isEqualTo(70);
    }
}

