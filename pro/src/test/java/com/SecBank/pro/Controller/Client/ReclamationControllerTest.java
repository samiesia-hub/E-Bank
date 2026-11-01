package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.ReclamationController;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Services.Clients.ReclamationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ReclamationController.class)
class ReclamationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReclamationService reclamationService;
    @Test
    void testCreerReclamation() throws Exception {

        Reclamation reclamation = new Reclamation();
        reclamation.setId(1L);
        reclamation.setObjet("Problème de virement");
        reclamation.setDesciprtion("Mon virement n'a pas été effectué.");
        reclamation.setDateCreation(new Date());

        when(reclamationService.creerReclamation(
                1L,
                "Problème de virement",
                "Mon virement n'a pas été effectué."
        )).thenReturn(reclamation);

        mockMvc.perform(post("/api/reclamations")
                        .param("clientId", "1")
                        .param("objet", "Problème de virement")
                        .param("description", "Mon virement n'a pas été effectué.")
                        .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.objet").value("Problème de virement"));
    }

    @Test
    void testSuivreReclamation() throws Exception {

        Reclamation reclamation = new Reclamation();
        reclamation.setId(1L);
        reclamation.setObjet("Problème de carte");

        when(reclamationService.suivreReclamation(1L))
                .thenReturn(reclamation);

        mockMvc.perform(get("/api/reclamations/1"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.objet").value("Problème de carte"));
    }
}