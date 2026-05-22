package com.example.shareholder.controller;

import com.example.shareholder.model.Person;
import com.example.shareholder.service.PersonService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PersonController verifying REST endpoint behaviour.
 */
@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonService personService;

    private Person samplePerson() {
        return new Person("Matti", "Meikäläinen", "matti@example.fi", "040-1111111",
                "Testikatu 1", "00100", "Helsinki", "010101-0101",
                100, BigDecimal.TEN, "FI2900000000000001");
    }

    @Test
    void should_ReturnPersonList_When_GetPersonsIsCalled() throws Exception {
        // Domain: shareholder list view requires all registered persons
        when(personService.getPersons()).thenReturn(List.of(samplePerson()));

        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstname").value("Matti"));
    }

    @Test
    void should_ReturnPerson_When_GetPersonByIdIsCalled() throws Exception {
        // Domain: person detail view requires lookup by id
        when(personService.getPersonById(1L)).thenReturn(samplePerson());

        mockMvc.perform(get("/api/persons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastname").value("Meikäläinen"));
    }

    @Test
    void should_ReturnTop5_When_GetTop5IsCalled() throws Exception {
        // Domain: dashboard chart requires top-5 shareholders
        when(personService.getTop5ShareholdersAndRest()).thenReturn(List.of());

        mockMvc.perform(get("/api/persons/top5"))
                .andExpect(status().isOk());
    }

    @Test
    void should_ReturnCreatedPerson_When_PostPersonIsCalled() throws Exception {
        // Domain: registering a new shareholder must persist and return the created record
        Person p = samplePerson();
        when(personService.addPerson(any())).thenReturn(p);

        mockMvc.perform(post("/api/persons/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname").value("Matti"));
    }

    @Test
    void should_ReturnUpdatedPerson_When_PutPersonIsCalled() throws Exception {
        // Domain: person edit must return the updated record
        Person p = samplePerson();
        when(personService.updatePerson(eq(1L), any())).thenReturn(p);

        mockMvc.perform(put("/api/persons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname").value("Matti"));
    }

    @Test
    void should_ReturnOk_When_DeletePersonIsCalled() throws Exception {
        // Domain: successful deletion must return 200 with confirmation message
        doNothing().when(personService).deletePerson(1L);

        mockMvc.perform(delete("/api/persons/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Henkilö poistettu onnistuneesti"));
    }
}

