package com.example.shareholder.controller;

import com.example.shareholder.model.ShareOwnership;
import com.example.shareholder.service.ShareOwnershipService;
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
 * Unit tests for ShareOwnershipController verifying REST endpoint behaviour.
 */
@WebMvcTest(ShareOwnershipController.class)
class ShareOwnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareOwnershipService shareOwnershipService;

    @Test
    void should_ReturnAllOwnerships_When_GetAllShareOwnershipsIsCalled() throws Exception {
        // Domain: shareholder registry shows all ownership blocks
        when(shareOwnershipService.getAllShareOwnerships()).thenReturn(List.of(new ShareOwnership()));

        mockMvc.perform(get("/api/shareownership/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void should_ReturnOwnership_When_GetByIdIsCalled() throws Exception {
        // Domain: ownership detail view requires lookup by id
        ShareOwnership ownership = new ShareOwnership();
        when(shareOwnershipService.getShareOwnershipById(1L)).thenReturn(ownership);

        mockMvc.perform(get("/api/shareownership/1"))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnCount_When_GetCountIsCalled() throws Exception {
        // Domain: dashboard metric for number of ownership blocks
        when(shareOwnershipService.getTotalShareOwnership()).thenReturn(5L);

        mockMvc.perform(get("/api/shareownership/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}

