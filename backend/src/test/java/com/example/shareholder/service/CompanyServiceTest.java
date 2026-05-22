package com.example.shareholder.service;

import com.example.shareholder.model.Company;
import com.example.shareholder.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompanyService covering company profile retrieval and update,
 * including the two update branches: existing company vs. first-time creation.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companyRepository);
    }

    // ── getCompany ──────────────────────────────────────────────────────────

    @Test
    void should_ReturnCompany_When_CompanyWithId1Exists() {
        // Domain: the application manages exactly one company record (id=1)
        Company company = new Company("PHZ Full Stack Oy", "2765147-9", "Helsinki", "https://phz.fi");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        Company result = service.getCompany();

        assertThat(result.getName()).isEqualTo("PHZ Full Stack Oy");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_CompanyNotFound() {
        // Domain: system must surface a meaningful error when company data is missing
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompany())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yhtiötä ei löytynyt");
    }

    // ── updateCompany – existing record ──────────────────────────────────────

    @Test
    void should_UpdateExistingCompanyFields_When_CompanyAlreadyExists() {
        // Domain: company profile edits must persist all four editable attributes
        Company existing = new Company("Old Name", "0000000-0", "Espoo", "https://old.fi");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Company updates = new Company("New Name", "1234567-8", "Helsinki", "https://new.fi");
        Company result = service.updateCompany(updates);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCompanyId()).isEqualTo("1234567-8");
        assertThat(result.getCity()).isEqualTo("Helsinki");
        assertThat(result.getUrl()).isEqualTo("https://new.fi");
    }

    @Test
    void should_SaveExistingEntityInstance_When_CompanyExists() {
        // Domain: update must mutate the same JPA entity to avoid losing other fields
        Company existing = new Company("Old Name", "0000000-0", "Espoo", "https://old.fi");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateCompany(new Company("New Name", "9999999-9", "Tampere", "https://tampere.fi"));

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    // ── updateCompany – first-time creation ─────────────────────────────────

    @Test
    void should_CreateNewCompany_When_NoCompanyExistsYet() {
        // Domain: first-run scenario — company record does not exist and must be bootstrapped
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Company newData = new Company("Bootstrap Oy", "7654321-0", "Oulu", "https://bootstrap.fi");
        Company result = service.updateCompany(newData);

        assertThat(result.getName()).isEqualTo("Bootstrap Oy");
        assertThat(result.getCity()).isEqualTo("Oulu");
    }

    @Test
    void should_SaveNewEntityInstance_When_CompanyDoesNotExist() {
        // Domain: fresh install must persist a new entity, not attempt update of null
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateCompany(new Company("New Co", "0000001-1", "Turku", "https://turku.fi"));

        verify(companyRepository, times(1)).save(any(Company.class));
    }
}

