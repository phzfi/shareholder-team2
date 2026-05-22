package com.example.shareholder.util;

import com.example.shareholder.model.Person;
import com.example.shareholder.model.SharePrice;
import com.example.shareholder.model.ShareTransaction;
import com.example.shareholder.service.PersonService;
import com.example.shareholder.service.SharePriceService;
import com.example.shareholder.service.ShareTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AddMultipleEntities utility controller.
 */
@WebMvcTest(AddMultipleEntities.class)
class AddMultipleEntitiesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonService personService;

    @MockBean
    private ShareTransactionService shareTransactionService;

    @MockBean
    private SharePriceService sharePriceService;

    private Person samplePerson() {
        return new Person("Matti", "M", "m@m.fi", "040", "Katu 1", "00100", "Helsinki",
                "010101-0101", 100, BigDecimal.ZERO, "FI00");
    }

    @Test
    void should_AddEachPerson_When_MultiplePersonsPosted() throws Exception {
        // Domain: bulk import adds each person in the list individually
        when(personService.addPerson(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Person> persons = List.of(samplePerson(), samplePerson());

        mockMvc.perform(post("/api/add-multiple-entities/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(persons)))
                .andExpect(status().isOk());

        verify(personService, times(2)).addPerson(any());
    }

    @Test
    void should_AddEachTransaction_When_MultipleTransactionsPosted() throws Exception {
        // Domain: bulk import adds each transaction individually
        when(shareTransactionService.addShareTransaction(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ShareTransaction> transactions = List.of(new ShareTransaction());

        mockMvc.perform(post("/api/add-multiple-entities/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transactions)))
                .andExpect(status().isOk());

        verify(shareTransactionService, times(1)).addShareTransaction(any());
    }

    @Test
    void should_AddEachSharePrice_When_MultipleSharePricesPosted() throws Exception {
        // Domain: bulk import adds each share price individually
        when(sharePriceService.addSharePrice(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SharePrice> prices = List.of(
                new SharePrice(new BigDecimal("10.00")),
                new SharePrice(new BigDecimal("11.00")));

        mockMvc.perform(post("/api/add-multiple-entities/shareprice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prices)))
                .andExpect(status().isOk());

        verify(sharePriceService, times(2)).addSharePrice(any());
    }
}

