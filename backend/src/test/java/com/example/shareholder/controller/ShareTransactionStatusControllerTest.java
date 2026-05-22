package com.example.shareholder.controller;

import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.service.ShareTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ShareTransactionStatusController verifying REST endpoint behaviour.
 */
@WebMvcTest(ShareTransactionStatusController.class)
class ShareTransactionStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareTransactionService shareTransactionService;

    @Test
    void should_ReturnPendingTransactions_When_PendingStatusIsRequested() throws Exception {
        // Domain: approvals dashboard filters transactions by status
        ShareTransaction tx = new ShareTransaction();
        when(shareTransactionService.getTransactionsByStatus("pending")).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/sharetransactionstatus/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void should_ReturnApprovedTransactions_When_ApprovedStatusIsRequested() throws Exception {
        // Domain: approved transaction list feeds the completed transfers view
        when(shareTransactionService.getTransactionsByStatus("approved")).thenReturn(List.of());

        mockMvc.perform(get("/api/sharetransactionstatus/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

