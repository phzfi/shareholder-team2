package com.example.shareholder.service;

import com.example.shareholder.model.Person;
import com.example.shareholder.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OwnerPercentageCalculator verifying share ownership percentage
 * recalculation across all branches: single owner, multiple owners, and zero total shares.
 */
@ExtendWith(MockitoExtension.class)
class OwnerPercentageCalculatorTest {

    @Mock
    private PersonRepository personRepository;

    private OwnerPercentageCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OwnerPercentageCalculator(personRepository);
    }

    private Person personWithShares(int shares) {
        return new Person("First", "Last", "e@e.fi", "0401234567",
                "Street 1", "00100", "Helsinki", "010101-1234",
                shares, BigDecimal.ZERO, "FI0000000000");
    }

    @Test
    void should_SetHundredPercentOwnership_When_OnlyOneShareholderExists() {
        // Domain: sole owner must hold exactly 100 % of the company
        Person sole = personWithShares(1000);
        when(personRepository.findAll()).thenReturn(List.of(sole));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        calculator.updateAllOwnershipPercentages();

        assertThat(sole.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void should_DistributeOwnershipProportionally_When_MultipleShareholdersExist() {
        // Domain: two equal partners each own 50 % when shares are split evenly
        Person alice = personWithShares(500);
        Person bob   = personWithShares(500);
        when(personRepository.findAll()).thenReturn(List.of(alice, bob));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        calculator.updateAllOwnershipPercentages();

        assertThat(alice.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("50.0000"));
        assertThat(bob.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    @Test
    void should_SetOwnershipToZero_When_TotalSharesIsZero() {
        // Domain: before any shares are issued, all ownership percentages must be 0
        Person person = personWithShares(0);
        when(personRepository.findAll()).thenReturn(List.of(person));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        calculator.updateAllOwnershipPercentages();

        assertThat(person.getOwnershipPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_HandleEmptyShareholderList_WithoutThrowing() {
        // Domain: system must not crash when no shareholders exist (empty company)
        when(personRepository.findAll()).thenReturn(Collections.emptyList());

        calculator.updateAllOwnershipPercentages();

        verify(personRepository, never()).save(any());
    }

    @Test
    void should_PersistUpdatedPercentage_ForEachShareholder() {
        // Domain: percentage changes must be written back to the database for each person
        Person alice = personWithShares(750);
        Person bob   = personWithShares(250);
        when(personRepository.findAll()).thenReturn(List.of(alice, bob));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        calculator.updateAllOwnershipPercentages();

        verify(personRepository, times(2)).save(any(Person.class));
        assertThat(alice.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("75.0000"));
        assertThat(bob.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("25.0000"));
    }

    @Test
    void should_RoundOwnershipToFourDecimalPlaces_When_DivisionIsRepeating() {
        // Domain: 1/3 ownership: divide(3, 4dp HALF_UP) = 0.3333, then multiply(100) = 33.3300
        // BigDecimal.multiply does not apply additional rounding, so the result is 33.3300
        Person a = personWithShares(1);
        Person b = personWithShares(1);
        Person c = personWithShares(1);
        when(personRepository.findAll()).thenReturn(List.of(a, b, c));
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        calculator.updateAllOwnershipPercentages();

        // 1/3 divide with 4dp HALF_UP = 0.3333; multiply by 100 = 33.3300
        assertThat(a.getOwnershipPercentage()).isEqualByComparingTo(new BigDecimal("33.3300"));
    }
}

