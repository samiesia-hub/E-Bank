package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie le contrat d'erreur des reponses : chaque exception metier garde son
 * code HTTP et un message fonctionnel, et une panne technique ne renvoie qu'un
 * message generique (la stack trace reste dans les logs serveur).
 */
@WebMvcTest(controllers = ApiExceptionHandlerTest.PanneController.class)
@Import(ApiExceptionHandlerTest.PanneController.class)
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class PanneController {

        @GetMapping("/test/panne")
        String panne() {
            throw new IllegalStateException("panne simulee avec un rib MA64-0111-0000-0000-0000-0042");
        }

        @GetMapping("/test/refus")
        String refus() {
            throw new AccesRefuseException("Ce compte appartient a un autre client");
        }

        @GetMapping("/test/introuvable")
        String introuvable() {
            throw new ResourceNotFoundException("Client introuvable avec id :9999");
        }

        @GetMapping("/test/invalide")
        String invalide() {
            throw new DonneeInvalideException("Le montant doit etre strictement positif");
        }
    }

    @Test
    @DisplayName("Given une panne technique, Then le client recoit un 500 generique et la stack trace reste dans les logs")
    void erreurInattendueRetourneUnMessageGenerique(CapturedOutput sortie) throws Exception {
        String corps = mockMvc.perform(get("/test/panne"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corps)
                .contains("Erreur interne du serveur")
                .doesNotContain("panne simulee", "IllegalStateException", "java.lang", "at com.SecBank", "MA64-0111");

        String logs = sortie.getOut() + sortie.getErr();
        assertThat(logs)
                .contains("Erreur inattendue sur GET /test/panne")
                .contains("IllegalStateException")
                .contains("panne simulee");
    }

    @Test
    @DisplayName("Given une ressource inconnue, Then 404 avec le motif")
    void ressourceInconnueRetourne404() throws Exception {
        mockMvc.perform(get("/test/introuvable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client introuvable avec id :9999"));
    }

    @Test
    @DisplayName("Given une ressource d'un autre client, Then 403 avec le motif")
    void accesRefuseRetourne403() throws Exception {
        mockMvc.perform(get("/test/refus"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Ce compte appartient a un autre client"));
    }

    @Test
    @DisplayName("Given une donnee invalide, Then 400 avec le motif")
    void donneeInvalideRetourne400() throws Exception {
        mockMvc.perform(get("/test/invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le montant doit etre strictement positif"));
    }
}
