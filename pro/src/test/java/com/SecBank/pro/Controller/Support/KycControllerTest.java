package com.SecBank.pro.Controller.Support;

import com.SecBank.pro.Conrollers.Support.KycController;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Enums.StatutKYC;
import com.SecBank.pro.Services.Admin.KycService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KycController.class)
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KycService kycService;

    @Test
    @DisplayName("Given des dossiers en attente, When le support liste, Then 200 avec le statut EN_ATTENTE")
    void dossiersEnAttente() throws Exception {
        when(kycService.listerDossiersEnAttente()).thenReturn(List.of(dossier(StatutKYC.EN_ATTENTE)));

        mockMvc.perform(get("/api/support/kyc").principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$[0].clientEmail").value("jean@example.com"));
    }

    @Test
    @DisplayName("Given l'alias /pending, When le support liste, Then la meme liste revient")
    void aliasPending() throws Exception {
        when(kycService.listerDossiersEnAttente()).thenReturn(List.of(dossier(StatutKYC.EN_ATTENTE)));

        mockMvc.perform(get("/api/support/kyc/pending").principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Given un dossier, When l'agent le valide, Then 200 avec le statut VALIDE")
    void valider() throws Exception {
        when(kycService.validerDossierKYC(eq(4L), eq("agent@example.com"), anyString()))
                .thenReturn(dossier(StatutKYC.VALIDE));

        mockMvc.perform(post("/api/support/kyc/4/validate")
                        .principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));
    }

    @Test
    @DisplayName("Given un refus sans motif, When l'agent rejette, Then 400")
    void rejetSansMotifRenvoie400() throws Exception {
        when(kycService.rejeterDossierKYC(eq(4L), any(), anyString(), any()))
                .thenThrow(new DonneeInvalideException("Le motif de refus est obligatoire"));

        mockMvc.perform(post("/api/support/kyc/4/reject")
                        .principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le motif de refus est obligatoire"));
    }

    @Test
    @DisplayName("Given un motif, When l'agent rejette, Then 200 avec le statut REJETE")
    void rejetAvecMotif() throws Exception {
        when(kycService.rejeterDossierKYC(eq(4L), eq("Piece illisible"), eq("agent@example.com"), anyString()))
                .thenReturn(dossier(StatutKYC.REJETE));

        mockMvc.perform(post("/api/support/kyc/4/reject")
                        .principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Piece illisible\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"));
    }

    private static DossierKYC dossier(StatutKYC statut) {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");
        client.setNom("Jean");

        DossierKYC dossier = new DossierKYC();
        dossier.setId(4L);
        dossier.setStatutKYC(statut);
        dossier.setDateSoumission(LocalDateTime.now().minusDays(2));
        dossier.setClient(client);
        return dossier;
    }

    private static Principal principal(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
