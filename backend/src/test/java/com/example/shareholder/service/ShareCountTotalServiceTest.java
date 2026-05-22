package com.example.shareholder.service;

import com.example.shareholder.model.ShareCountTotal;
import com.example.shareholder.repository.ShareCountTotalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShareCountTotalService covering share count history management:
 * retrieving totals, adding to an existing total, and bootstrapping from empty state.
 */
@ExtendWith(MockitoExtension.class)
class ShareCountTotalServiceTest {

    @Mock
    private ShareCountTotalRepository shareCountTotalRepository;

    private ShareCountTotalService service;

    @BeforeEach
    void setUp() {
        service = new ShareCountTotalService(shareCountTotalRepository);
    }

    // ── getAllTotalCounts ────────────────────────────────────────────────────

    @Test
    void should_ReturnAllHistoricalShareCountRecords_When_RecordsExist() {
        // Domain: audit trail of total share counts across time must be fully retrievable
        List<ShareCountTotal> records = List.of(new ShareCountTotal(100), new ShareCountTotal(200));
        when(shareCountTotalRepository.findAll()).thenReturn(records);

        List<ShareCountTotal> result = service.getAllTotalCounts();

        assertThat(result).hasSize(2);
    }

    // ── getLatestTotalCount ──────────────────────────────────────────────────

    @Test
    void should_ReturnLatestShareCount_When_AtLeastOneRecordExists() {
        // Domain: dashboard needs the most-recent total share count
        ShareCountTotal latest = new ShareCountTotal(500);
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(latest));

        ShareCountTotal result = service.getLatestTotalCount();

        assertThat(result.getTotalShares()).isEqualTo(500);
    }

    @Test
    void should_ThrowRuntimeException_When_NoShareCountRecordExists() {
        // Domain: system must fail visibly if share count history is missing
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestTotalCount())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Osakkeiden kokonaismäärää ei löydy");
    }

    // ── addTotalShareCount ───────────────────────────────────────────────────

    @Test
    void should_AddSharesOnTopOfExistingTotal_When_PreviousTotalExists() {
        // Domain: issuing new shares appends to running total (e.g., 300 + 200 = 500)
        ShareCountTotal existing = new ShareCountTotal(300);
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing));
        when(shareCountTotalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Integer result = service.addTotalShareCount(200);

        assertThat(result).isEqualTo(500);
    }

    @Test
    void should_SaveNewRecordWithZero_When_NoPreviousTotalExists() {
        // Domain: first share issuance bootstraps the counter from 0, not from the new count
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(shareCountTotalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Integer result = service.addTotalShareCount(100);

        assertThat(result).isEqualTo(0);
        ArgumentCaptor<ShareCountTotal> captor = ArgumentCaptor.forClass(ShareCountTotal.class);
        verify(shareCountTotalRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalShares()).isEqualTo(0);
    }

    @ParameterizedTest(name = "adding {0} shares to existing total of 1000")
    @ValueSource(ints = {0, 1, 999, 10000})
    void should_AccumulateSharesCorrectly_ForBoundaryShareCounts(int additionalShares) {
        // Domain: boundary value analysis — zero shares, minimum, large batches
        ShareCountTotal existing = new ShareCountTotal(1000);
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing));
        when(shareCountTotalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Integer result = service.addTotalShareCount(additionalShares);

        assertThat(result).isEqualTo(1000 + additionalShares);
    }

    @Test
    void should_PersistNewTotalRecord_When_SharesAreAdded() {
        // Domain: each share issuance must create an immutable audit record
        ShareCountTotal existing = new ShareCountTotal(100);
        when(shareCountTotalRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing));
        when(shareCountTotalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addTotalShareCount(50);

        ArgumentCaptor<ShareCountTotal> captor = ArgumentCaptor.forClass(ShareCountTotal.class);
        verify(shareCountTotalRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalShares()).isEqualTo(150);
    }

    // ── ShareCountTotal model constructors ──────────────────────────────────

    @Test
    void should_StoreSharesAndDate_When_ConstructorWithDateIsUsed() {
        // Domain: audit record must capture the exact date shares were added
        java.time.LocalDate date = java.time.LocalDate.of(2024, 6, 15);
        ShareCountTotal sct = new ShareCountTotal(500, date);

        assertThat(sct.getTotalShares()).isEqualTo(500);
        assertThat(sct.getDate()).isEqualTo(date);
    }

    @Test
    void should_SetAndGetDate_When_DateSetterIsUsed() {
        // Domain: date on the audit record must be modifiable for corrections
        ShareCountTotal sct = new ShareCountTotal(100);
        java.time.LocalDate newDate = java.time.LocalDate.of(2025, 1, 1);
        sct.setDate(newDate);

        assertThat(sct.getDate()).isEqualTo(newDate);
    }

    @Test
    void should_UpdateTotalShares_When_SetTotalSharesIsUsed() {
        // Domain: share total can be corrected via the setter for administrative adjustments
        ShareCountTotal sct = new ShareCountTotal(100);
        sct.setTotalShares(250);
        assertThat(sct.getTotalShares()).isEqualTo(250);
    }
}

