package com.example.shareholder.service;

import com.example.shareholder.model.SharePrice;
import com.example.shareholder.repository.SharePriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SharePriceService covering share price CRUD, latest-price retrieval,
 * average-per-year calculation, and difference stamping on add/update.
 */
@ExtendWith(MockitoExtension.class)
class SharePriceServiceTest {

    @Mock private SharePriceRepository sharePriceRepository;
    @Mock private SharePriceDifferenceCalculator sharePriceDifferenceCalculator;

    private SharePriceService service;

    @BeforeEach
    void setUp() {
        service = new SharePriceService(sharePriceRepository, sharePriceDifferenceCalculator);
    }

    // ── getLatestPrice ───────────────────────────────────────────────────────

    @Test
    void should_ReturnLatestSharePrice_When_AtLeastOnePriceEntryExists() {
        // Domain: current share price is the most recently recorded entry
        SharePrice latest = new SharePrice(new BigDecimal("12.50"));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(latest));

        SharePrice result = service.getLatestPrice();

        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    void should_ReturnEmptySharePrice_When_NoPriceEntryExists() {
        // Domain: before first pricing event, a safe empty default is returned instead of null
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        SharePrice result = service.getLatestPrice();

        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getSharePrice ────────────────────────────────────────────────────────

    @Test
    void should_ReturnSharePrice_When_PriceWithGivenIdExists() {
        // Domain: historical price lookup by identifier for audit purposes
        SharePrice sp = new SharePrice(new BigDecimal("8.00"));
        when(sharePriceRepository.findById(1L)).thenReturn(Optional.of(sp));

        assertThat(service.getSharePrice(1L).getPrice()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    void should_ThrowRuntimeException_When_SharePriceIdDoesNotExist() {
        // Domain: referencing a non-existent price record must produce a clear error
        when(sharePriceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSharePrice(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ── addSharePrice ────────────────────────────────────────────────────────

    @Test
    void should_StampDifferenceAndPersist_When_AddingNewSharePrice() {
        // Domain: each price entry records the delta vs. the previous price for trend analysis
        SharePrice newPrice = new SharePrice(new BigDecimal("15.00"));
        when(sharePriceDifferenceCalculator.calculateDifference(newPrice))
                .thenReturn(new BigDecimal("2.50"));
        when(sharePriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SharePrice result = service.addSharePrice(newPrice);

        assertThat(result.getDifference()).isEqualByComparingTo(new BigDecimal("2.50"));
        verify(sharePriceRepository).save(newPrice);
    }

    // ── updateSharePrice ─────────────────────────────────────────────────────

    @Test
    void should_UpdatePriceAndRecalculateDifference_When_SharePriceExists() {
        // Domain: price corrections must refresh the difference and reset the date to today
        SharePrice existing = new SharePrice(new BigDecimal("10.00"), LocalDate.of(2024, 1, 1), BigDecimal.ZERO);
        when(sharePriceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sharePriceDifferenceCalculator.calculateDifference(any())).thenReturn(new BigDecimal("3.00"));
        when(sharePriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SharePrice updated = service.updateSharePrice(1L, new SharePrice(new BigDecimal("13.00")));

        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("13.00"));
        assertThat(updated.getDifference()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(updated.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void should_ThrowRuntimeException_When_UpdatingNonExistentSharePrice() {
        // Domain: updating an unknown price record must fail with a clear error
        when(sharePriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSharePrice(99L, new SharePrice(BigDecimal.ONE)))
                .isInstanceOf(RuntimeException.class);
    }

    // ── getAveragePricePerYear ────────────────────────────────────────────────

    @Test
    void should_ReturnAveragePriceMap_When_HistoricalDataExists() {
        // Domain: yearly average price chart requires grouped aggregation from the database
        when(sharePriceRepository.findAveragePricePerYear())
                .thenReturn(List.of(new Object[]{2023, 10.5}, new Object[]{2024, 12.0}));

        Map<Integer, BigDecimal> result = service.getAveragePricePerYear();

        assertThat(result).containsKey(2023);
        assertThat(result.get(2023)).isEqualByComparingTo(new BigDecimal("10.5"));
        assertThat(result.get(2024)).isEqualByComparingTo(new BigDecimal("12.0"));
    }

    @Test
    void should_ReturnEmptyMap_When_NoPriceHistoryExists() {
        // Domain: empty chart must be returned when no price data has been recorded yet
        when(sharePriceRepository.findAveragePricePerYear()).thenReturn(List.of());

        Map<Integer, BigDecimal> result = service.getAveragePricePerYear();

        assertThat(result).isEmpty();
    }

    // ── deleteSharePrice ──────────────────────────────────────────────────────

    @Test
    void should_DeleteSharePrice_When_IdIsValid() {
        // Domain: price records may be removed as part of data correction
        service.deleteSharePrice(5L);
        verify(sharePriceRepository).deleteById(5L);
    }

    // ── getAllSharePrices ─────────────────────────────────────────────────────

    @Test
    void should_ReturnAllSharePrices_When_GetAllSharePricesIsCalled() {
        // Domain: full price history is needed for trend charts
        SharePrice sp1 = new SharePrice(new BigDecimal("10.00"));
        SharePrice sp2 = new SharePrice(new BigDecimal("12.00"));
        when(sharePriceRepository.findAll()).thenReturn(List.of(sp1, sp2));

        Iterable<SharePrice> result = service.getAllSharePrices();

        assertThat(result).containsExactly(sp1, sp2);
    }
}

