package com.example.shareholder.controller;

import com.example.shareholder.model.ShareCountTotal;
import com.example.shareholder.service.ShareCountTotalService;
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
 * Unit tests for ShareCountTotalController verifying REST endpoint behaviour.
 */
@WebMvcTest(ShareCountTotalController.class)
class ShareCountTotalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareCountTotalService shareCountTotalService;

    @Test
    void should_ReturnAllCounts_When_GetAllTotalCountsIsCalled() throws Exception {
        // Domain: share count history is needed for trend graphs
        when(shareCountTotalService.getAllTotalCounts()).thenReturn(List.of(new ShareCountTotal()));

        mockMvc.perform(get("/api/totalshares/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void should_ReturnLatestCount_When_GetLatestTotalCountIsCalled() throws Exception {
        // Domain: dashboard shows the current total number of issued shares
        ShareCountTotal latest = new ShareCountTotal();
        when(shareCountTotalService.getLatestTotalCount()).thenReturn(latest);

        mockMvc.perform(get("/api/totalshares/latest"))
                .andExpect(status().isOk());
    }
}

