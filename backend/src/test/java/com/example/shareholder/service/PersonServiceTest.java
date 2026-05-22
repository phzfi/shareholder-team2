package com.example.shareholder.service;

import com.example.shareholder.model.Person;
import com.example.shareholder.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PersonService covering shareholder lifecycle operations:
 * creation validations, duplicate SSN detection, update logic, and deletion.
 */
@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock private PersonRepository personRepository;
    @Mock private ShareOwnershipService shareOwnershipService;
    @Mock private ShareCountTotalService shareCountTotalService;
    @Mock private OwnerPercentageCalculator ownerPercentageCalculator;

    private PersonService service;

    @BeforeEach
    void setUp() {
        service = new PersonService(personRepository, shareOwnershipService,
                shareCountTotalService, ownerPercentageCalculator);
    }

    private Person validPerson(int shares) {
        return new Person("Matti", "Meikäläinen", "matti@example.fi", "040-1234567",
                "Mannerheimintie 1", "00100", "Helsinki", "010170-1234",
                shares, BigDecimal.ZERO, "FI0000000000000000");
    }

    // ── getPersons ───────────────────────────────────────────────────────────

    @Test
    void should_ReturnAllShareholders_When_PersonsExistInDatabase() {
        // Domain: shareholder list must reflect all registered persons
        when(personRepository.findAll()).thenReturn(List.of(validPerson(100)));
        assertThat(service.getPersons()).hasSize(1);
    }

    // ── getPersonById ────────────────────────────────────────────────────────

    @Test
    void should_ReturnPerson_When_PersonWithGivenIdExists() {
        // Domain: detail view must fetch exact shareholder by identity
        Person p = validPerson(50);
        when(personRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThat(service.getPersonById(1L)).isSameAs(p);
    }

    @Test
    void should_ThrowIllegalArgumentException_When_PersonIdDoesNotExist() {
        // Domain: looking up a non-existent shareholder must produce an actionable error
        when(personRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPersonById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Henkilöä ei löytynyt id:llä 99");
    }

    // ── addPerson – validation branches ─────────────────────────────────────

    @Test
    void should_ThrowIllegalArgumentException_When_RequiredFieldIsNull() {
        // Domain: incomplete shareholder data must be rejected before persistence
        Person incomplete = new Person(null, "Last", "e@e.fi", "040",
                "Addr", "00100", "City", "111111-111A", 0, BigDecimal.ZERO, "FI00");
        assertThatThrownBy(() -> service.addPerson(incomplete))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kentät ovat pakollisia");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_SsnAlreadyRegistered() {
        // Domain: SSN is a unique national identifier; duplicate registration must be prevented
        Person duplicate = validPerson(0);
        when(personRepository.findBySsn("010170-1234")).thenReturn(duplicate);
        assertThatThrownBy(() -> service.addPerson(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("samalla henkilötunnuksella");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_NumberOfSharesIsNegative() {
        // Domain: negative share allocation is a logical impossibility
        Person p = validPerson(-1);
        when(personRepository.findBySsn(any())).thenReturn(null);
        assertThatThrownBy(() -> service.addPerson(p))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Osakemäärän on oltava nolla tai suurempi");
    }

    @Test
    void should_SavePersonAndUpdateShareOwnership_When_PersonHasShares() {
        // Domain: new shareholder with shares triggers ownership record and percentage update
        Person p = validPerson(100);
        when(personRepository.findBySsn(any())).thenReturn(null);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addPerson(p);

        verify(shareOwnershipService).addShareOwnership(p);
        verify(ownerPercentageCalculator).updateAllOwnershipPercentages();
        verify(shareCountTotalService).addTotalShareCount(100);
    }

    @Test
    void should_SavePersonWithoutOwnershipRecord_When_PersonHasZeroShares() {
        // Domain: observer / non-shareholder registration skips ownership creation
        Person p = validPerson(0);
        when(personRepository.findBySsn(any())).thenReturn(null);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addPerson(p);

        verify(shareOwnershipService, never()).addShareOwnership(any());
        verify(ownerPercentageCalculator, never()).updateAllOwnershipPercentages();
    }

    // ── updatePerson ─────────────────────────────────────────────────────────

    @Test
    void should_UpdateAllFields_When_PersonExistsAndSsnIsUnique() {
        // Domain: profile edit must persist all changed fields for an existing shareholder
        Person existing = validPerson(100);
        Person updates = new Person("Uusi", "Nimi", "uusi@example.fi", "050-9999999",
                "Uusi Katu 2", "00200", "Espoo", "020202-2222",
                100, java.math.BigDecimal.ZERO, "FI1111111111111111");
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.findBySsn("020202-2222")).thenReturn(null);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePerson(1L, updates);

        verify(personRepository).save(existing);
        assertThat(existing.getFirstname()).isEqualTo("Uusi");
        assertThat(existing.getSsn()).isEqualTo("020202-2222");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UpdatedSsnAlreadyTaken() {
        // Domain: SSN uniqueness must be enforced on update as well as create
        Person existing = validPerson(100);
        Person updates = validPerson(100); // same SSN as another registered person
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.findBySsn(any())).thenReturn(new Person()); // duplicate found

        assertThatThrownBy(() -> service.updatePerson(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("samalla henkilötunnuksella");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_UpdatingNonExistentPerson() {
        // Domain: updating a non-existent shareholder must fail with a clear error
        when(personRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updatePerson(42L, validPerson(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("42");
    }

    @Test
    void should_CallAddTotalShareCount_When_UpdateIsCalled() {
        // Domain: share-count is re-totalled after every person update
        Person existing = validPerson(100);
        Person updates = validPerson(200);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.findBySsn(any())).thenReturn(null);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePerson(1L, updates);

        verify(shareCountTotalService).addTotalShareCount(200);
        verify(ownerPercentageCalculator).updateAllOwnershipPercentages();
    }

    @Test
    void should_NotUpdateOwnershipPercentages_When_ShareCountIsUnchanged() {
        // Domain: ownership percentages should not be recalculated if no shares changed hands
        Person existing = validPerson(100);
        Person updates = new Person("Uusi", "Nimi", "u@u.fi", "050",
                "Tie 2", "00200", "Espoo", "020202-2222",
                100, java.math.BigDecimal.ZERO, "FI02");
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.findBySsn(any())).thenReturn(null);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePerson(1L, updates);

        verify(ownerPercentageCalculator, never()).updateAllOwnershipPercentages();
    }

    // ── getTop5ShareholdersAndRest ────────────────────────────────────────────

    @Test
    void should_ReturnTop5Data_When_Called() {
        // Domain: dashboard pie chart requires the top-5 query result
        when(personRepository.findTop5ShareholdersAndRest())
                .thenReturn(List.of(new Object[]{}, new Object[]{}));
        assertThat(service.getTop5ShareholdersAndRest()).hasSize(2);
    }

    // ── deletePerson ─────────────────────────────────────────────────────────

    @Test
    void should_DeletePerson_When_PersonExists() {
        // Domain: removing a deregistered shareholder must clean up the record
        Person p = validPerson(0);
        when(personRepository.findById(1L)).thenReturn(Optional.of(p));
        service.deletePerson(1L);
        verify(personRepository).deleteById(any());
    }

    @Test
    void should_ThrowIllegalArgumentException_When_DeletingNonExistentPerson() {
        // Domain: attempt to delete an unknown shareholder must fail gracefully
        when(personRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deletePerson(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Henkilöä ei löytynyt id:llä 999");
    }
}

