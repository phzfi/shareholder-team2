package com.example.shareholder.controller;

import com.example.shareholder.model.SharePrice;
import com.example.shareholder.service.SharePriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SharePriceController verifying REST endpoint behaviour.
 */
@WebMvcTest(SharePriceController.class)
class SharePriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SharePriceService sharePriceService;

    private SharePrice samplePrice() {
        return new SharePrice(new BigDecimal("12.50"), LocalDate.of(2024, 1, 1), BigDecimal.ZERO);
    }

    @Test
    void should_ReturnAllPrices_When_GetAllSharePricesIsCalled() throws Exception {
        // Domain: price history chart requires all historic prices
        when(sharePriceService.getAllSharePrices()).thenReturn(List.of(samplePrice()));

        mockMvc.perform(get("/api/shareprice/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void should_ReturnLatestPrice_When_GetLatestPriceIsCalled() throws Exception {
        // Domain: current share price is shown on the dashboard
        when(sharePriceService.getLatestPrice()).thenReturn(samplePrice());

        mockMvc.perform(get("/api/shareprice/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(12.50));
    }

    @Test
    void should_ReturnPrice_When_GetSharePriceByIdIsCalled() throws Exception {
        // Domain: price detail view requires lookup by id
        when(sharePriceService.getSharePrice(1L)).thenReturn(samplePrice());

        mockMvc.perform(get("/api/shareprice/1"))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnAveragePricePerYear_When_GetAveragePricePerYearIsCalled() throws Exception {
        // Domain: yearly average is used in financial trend analysis
        when(sharePriceService.getAveragePricePerYear())
                .thenReturn(Map.of(2024, new BigDecimal("11.00")));

        mockMvc.perform(get("/api/shareprice/averageperyear"))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnCreatedPrice_When_PostSharePriceIsCalled() throws Exception {
        // Domain: adding a new price record updates the price timeline
        SharePrice p = samplePrice();
        when(sharePriceService.addSharePrice(any())).thenReturn(p);

        mockMvc.perform(post("/api/shareprice/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnUpdatedPrice_When_PutSharePriceIsCalled() throws Exception {
        // Domain: correcting a mis-entered price must persist and return the updated record
        SharePrice p = samplePrice();
        when(sharePriceService.updateSharePrice(eq(1L), any())).thenReturn(p);

        mockMvc.perform(put("/api/shareprice/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnOk_When_DeleteSharePriceIsCalled() throws Exception {
        // Domain: deleting a price entry must succeed with 200 OK
        doNothing().when(sharePriceService).deleteSharePrice(1L);

        mockMvc.perform(delete("/api/shareprice/1"))
                .andExpect(status().isOk());
    }
}

