package com.example.shareholder.service;

import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.repository.ShareTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShareCalculatorService verifying total-share summation logic
 * across boundary cases: empty history, single transaction, and multiple transactions.
 */
@ExtendWith(MockitoExtension.class)
class ShareCalculatorServiceTest {

    @Mock
    private ShareTransactionRepository shareTransactionRepository;

    private ShareCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new ShareCalculatorService(shareTransactionRepository);
    }

    private ShareTransaction txWithShares(int shares) {
        ShareTransaction tx = new ShareTransaction();
        tx.setNumberOfShares(shares);
        return tx;
    }

    @Test
    void should_ReturnZero_When_NoTransactionsExist() {
        // Domain: no transactions means no shares have been issued or traded
        when(shareTransactionRepository.findAll()).thenReturn(List.of());

        Integer result = service.calculateTotalShares();

        assertThat(result).isEqualTo(0);
    }

    @Test
    void should_ReturnShareCount_When_SingleTransactionExists() {
        // Domain: a single transaction contribution must be returned unchanged
        when(shareTransactionRepository.findAll()).thenReturn(List.of(txWithShares(250)));

        Integer result = service.calculateTotalShares();

        assertThat(result).isEqualTo(250);
    }

    @Test
    void should_SumAllTransactionShares_When_MultipleTransactionsExist() {
        // Domain: total must aggregate shares across all historical transactions
        when(shareTransactionRepository.findAll())
                .thenReturn(List.of(txWithShares(100), txWithShares(200), txWithShares(50)));

        Integer result = service.calculateTotalShares();

        assertThat(result).isEqualTo(350);
    }

    @Test
    void should_IncludeAllTransactionsInSum_When_ValuesVary() {
        // Domain: large and small transactions must all contribute to the total
        when(shareTransactionRepository.findAll())
                .thenReturn(List.of(txWithShares(1), txWithShares(9999)));

        Integer result = service.calculateTotalShares();

        assertThat(result).isEqualTo(10000);
    }
}

