package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.ReclamationController;
import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Services.Clients.ReclamationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReclamationController.class)
class ReclamationControllerTest {

    private static final String EMAIL = "jean@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReclamationService reclamationService;

    @Test
    void creerReclamation_valide_retourne201EnCours() throws Exception {
        when(reclamationService.creerReclamation(
                EMAIL, "Problème de virement", "Mon virement n'a pas été effectué."))
                .thenReturn(reclamation(1L, "Problème de virement",
                        "Mon virement n'a pas été effectué.", StatusReclamation.EN_COURS));

        mockMvc.perform(post("/api/client/reclamations")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objet\":\"Problème de virement\",\"message\":\"Mon virement n'a pas été effectué.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.objet").value("Problème de virement"))
                .andExpect(jsonPath("$.message").value("Mon virement n'a pas été effectué."))
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.dateCreation").exists());
    }

    @Test
    void creerReclamation_messageTropCourt_retourne400() throws Exception {
        when(reclamationService.creerReclamation(eq(EMAIL), any(), any()))
                .thenThrow(new DonneeInvalideException("Le message doit contenir au moins 10 caractères"));

        mockMvc.perform(post("/api/client/reclamations")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objet\":\"Objet\",\"message\":\"court\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le message doit contenir au moins 10 caractères"));
    }

    @Test
    void mesReclamations_listeCellesDuClientConnecte() throws Exception {
        when(reclamationService.listerReclamationsDuClient(EMAIL)).thenReturn(List.of(
                reclamation(2L, "Carte avalée", "Ma carte a été avalée au distributeur.", StatusReclamation.EN_COURS),
                reclamation(1L, "Frais", "Des frais inattendus ont été prélevés.", StatusReclamation.RESOLUE)));

        mockMvc.perform(get("/api/client/reclamations").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].statut").value("RESOLUE"));
    }

    @Test
    void suivreReclamation_autreClient_retourne403() throws Exception {
        when(reclamationService.consulterReclamationDuClient(7L, EMAIL))
                .thenThrow(new AccesRefuseException("Cette réclamation n'appartient pas au client connecté"));

        mockMvc.perform(get("/api/client/reclamations/7").principal(principal()))
                .andExpect(status().isForbidden());
    }

    /**
     * Remplace l'ancien test de régression (boucle JSON client -> comptes -> ...) :
     * la réponse est maintenant un DTO, qui n'expose ni le client ni le ticket.
     */
    @Test
    void suivreReclamation_neRenvoiePasLeClientNiLeTicket() throws Exception {
        Client client = new Client();
        client.setEmail(EMAIL);
        client.setPassword("hash-secret");
        Reclamation reclamation = reclamation(4001L, "Frais bancaires",
                "Des frais inattendus ont été prélevés.", StatusReclamation.EN_COURS);
        reclamation.setClient(client);
        when(reclamationService.consulterReclamationDuClient(4001L, EMAIL)).thenReturn(reclamation);

        mockMvc.perform(get("/api/client/reclamations/4001").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objet").value("Frais bancaires"))
                .andExpect(jsonPath("$.client").doesNotExist())
                .andExpect(jsonPath("$.ticketSupport").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private static Principal principal() {
        return new UsernamePasswordAuthenticationToken(
                EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private static Reclamation reclamation(Long id, String objet, String message, StatusReclamation statut) {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(id);
        reclamation.setObjet(objet);
        reclamation.setDesciprtion(message);
        reclamation.setStatus(statut);
        reclamation.setDateCreation(new Date());
        return reclamation;
    }
}