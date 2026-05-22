package com.example.shareholder.controller;

import com.example.shareholder.model.Person;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.repository.PersonRepository;
import com.example.shareholder.service.ReportService;
import com.example.shareholder.service.ShareTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ReportController verifying that Excel export endpoints call the report service.
 */
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private PersonRepository personRepository;

    @MockBean
    private ShareTransactionService shareTransactionService;

    private Person samplePerson() {
        return new Person("Matti", "Meikäläinen", "m@m.fi", "040-0000001",
                "Katu 1", "00100", "Helsinki", "010101-0101",
                100, BigDecimal.TEN, "FI00");
    }

    @Test
    void should_CallReportService_When_PersonsExportIsRequested() throws Exception {
        // Domain: export triggers the Excel generation pipeline for persons data
        when(personRepository.findAll()).thenReturn(List.of(samplePerson()));
        doNothing().when(reportService).exportToExcel(any(), any(), any(), any());

        mockMvc.perform(get("/api/report/persons"))
                .andExpect(status().isOk());

        verify(reportService).exportToExcel(any(), any(), any(), eq("Osakasluettelo"));
    }

    private ShareTransaction sampleTransaction() {
        Person seller = samplePerson();
        Person buyer = new Person("Maija", "Virtanen", "v@v.fi", "041-1111111",
                "Tie 2", "00200", "Espoo", "020202-0202",
                0, BigDecimal.ZERO, "FI01");
        ShareTransaction tx = new ShareTransaction(
                LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15),
                seller, buyer, true, 10,
                new BigDecimal("10.00"), new BigDecimal("100.00"), "testihuomio");
        return tx;
    }

    @Test
    void should_CallReportService_When_TransactionsExportIsRequested() throws Exception {
        // Domain: export triggers the Excel generation pipeline for transactions data
        when(shareTransactionService.getShareTransactions()).thenReturn(List.of(sampleTransaction()));
        doNothing().when(reportService).exportToExcel(any(), any(), any(), any());

        mockMvc.perform(get("/api/report/transactions"))
                .andExpect(status().isOk());

        verify(reportService).exportToExcel(any(), any(), any(), eq("Merkintähistoria"));
    }
}

