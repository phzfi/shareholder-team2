package com.example.shareholder.service;

import com.example.shareholder.model.SharePrice;
import com.example.shareholder.repository.SharePriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SharePriceDifferenceCalculator covering
 * all branches: no previous price, positive difference, negative difference, zero difference.
 */
@ExtendWith(MockitoExtension.class)
class SharePriceDifferenceCalculatorTest {

    @Mock
    private SharePriceRepository sharePriceRepository;

    private SharePriceDifferenceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SharePriceDifferenceCalculator(sharePriceRepository);
    }

    @Test
    void should_ReturnZero_When_NoPreviousSharePriceExists() {
        // Domain: first ever share price entry has no baseline to compare against
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        SharePrice newPrice = new SharePrice(new BigDecimal("10.00"));
        BigDecimal result = calculator.calculateDifference(newPrice);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_ReturnPositiveDifference_When_SharePriceIncreases() {
        // Domain: price went up from 10.00 to 15.00 — difference should be +5.00
        SharePrice previous = new SharePrice(new BigDecimal("10.00"));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(previous));

        SharePrice newPrice = new SharePrice(new BigDecimal("15.00"));
        BigDecimal result = calculator.calculateDifference(newPrice);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void should_ReturnNegativeDifference_When_SharePriceDecreases() {
        // Domain: price dropped from 20.00 to 12.50 — difference should be -7.50
        SharePrice previous = new SharePrice(new BigDecimal("20.00"));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(previous));

        SharePrice newPrice = new SharePrice(new BigDecimal("12.50"));
        BigDecimal result = calculator.calculateDifference(newPrice);

        assertThat(result).isEqualByComparingTo(new BigDecimal("-7.50"));
    }

    @Test
    void should_ReturnZeroDifference_When_SharePriceIsUnchanged() {
        // Domain: price stays the same — difference must be exactly 0.00
        SharePrice previous = new SharePrice(new BigDecimal("8.75"));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(previous));

        SharePrice newPrice = new SharePrice(new BigDecimal("8.75"));
        BigDecimal result = calculator.calculateDifference(newPrice);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "previous={0}, new={1}, expected={2}")
    @CsvSource({
        "100.00, 110.00, 10.00",   // 10% price gain
        "100.00, 90.00,  -10.00",  // 10% price drop
        "0.01,   0.02,   0.01",    // boundary: smallest price increment
        "0.00,   0.00,   0.00",    // boundary: zero-priced shares
        "999.99, 1000.00, 0.01",   // boundary: 1-cent gain near maximum
    })
    void should_ComputeDifferenceCorrectly_ForBoundaryAndTypicalValues(
            String previousStr, String newStr, String expectedStr) {
        // Domain: parameterised boundary value analysis for share price movement
        SharePrice previous = new SharePrice(new BigDecimal(previousStr));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(previous));

        SharePrice newPrice = new SharePrice(new BigDecimal(newStr));
        BigDecimal result = calculator.calculateDifference(newPrice);

        assertThat(result).isEqualByComparingTo(new BigDecimal(expectedStr));
    }

    @Test
    void should_RoundDifferenceToTwoDecimalPlaces_When_PriceHasMorePrecision() {
        // Domain: share prices are stored at 2dp; rounding must use HALF_UP
        SharePrice previous = new SharePrice(new BigDecimal("10.005"));
        when(sharePriceRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(previous));

        SharePrice newPrice = new SharePrice(new BigDecimal("10.015"));
        BigDecimal result = calculator.calculateDifference(newPrice);

        // 10.015 - 10.005 = 0.010, rounded HALF_UP to 2dp => 0.01
        assertThat(result.scale()).isEqualTo(2);
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.01"));
    }
}

