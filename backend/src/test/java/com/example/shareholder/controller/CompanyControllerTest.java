package com.example.shareholder.controller;

import com.example.shareholder.model.Company;
import com.example.shareholder.service.CompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CompanyController verifying REST endpoint behaviour.
 */
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    private Company sampleCompany() {
        return new Company("Testi Oy", "1234567-8", "Helsinki", "https://testi.fi");
    }

    @Test
    void should_ReturnCompany_When_GetCompanyIsCalled() throws Exception {
        // Domain: shareholders need to see the registered company details
        when(companyService.getCompany()).thenReturn(sampleCompany());

        mockMvc.perform(get("/api/company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Testi Oy"));
    }

    @Test
    void should_ReturnUpdatedCompany_When_PutCompanyIsCalled() throws Exception {
        // Domain: admin can update company info; response must reflect the new values
        Company updated = sampleCompany();
        when(companyService.updateCompany(any())).thenReturn(updated);

        mockMvc.perform(put("/api/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value("1234567-8"));
    }
}

