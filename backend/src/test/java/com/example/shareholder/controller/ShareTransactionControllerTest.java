package com.example.shareholder.controller;

import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.service.ShareTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ShareTransactionController verifying REST endpoint behaviour.
 */
@WebMvcTest(ShareTransactionController.class)
class ShareTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShareTransactionService shareTransactionService;

    @Test
    void should_ReturnAllTransactions_When_GetTransactionsIsCalled() throws Exception {
        // Domain: transaction history view requires all recorded transfers
        when(shareTransactionService.getShareTransactions()).thenReturn(List.of(new ShareTransaction()));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void should_ReturnTransaction_When_GetByIdIsCalled() throws Exception {
        // Domain: transaction detail view requires lookup by id
        when(shareTransactionService.getShareTransactionById(1L)).thenReturn(new ShareTransaction());

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnCreatedTransaction_When_PostTransactionIsCalled() throws Exception {
        // Domain: submitting a share transfer must persist and return the new transaction
        ShareTransaction tx = new ShareTransaction();
        when(shareTransactionService.addShareTransaction(any())).thenReturn(tx);

        mockMvc.perform(post("/api/transactions/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnUpdatedTransaction_When_PutTransactionIsCalled() throws Exception {
        // Domain: updating a transaction must persist and return the modified record
        ShareTransaction tx = new ShareTransaction();
        when(shareTransactionService.updateShareTransaction(eq(1L), any())).thenReturn(tx);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnOkWithMessage_When_DeleteTransactionIsCalled() throws Exception {
        // Domain: successful deletion must return 200 with confirmation message
        doNothing().when(shareTransactionService).deleteShareTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaktio poistettu onnistuneesti"));
    }

    @Test
    void should_ReturnSearchResults_When_SearchQueryIsProvided() throws Exception {
        // Domain: transaction history can be filtered by seller/buyer name
        when(shareTransactionService.searchShareTransactions("Matti")).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/search").param("search", "Matti"))
                .andExpect(status().isOk());
    }
}

