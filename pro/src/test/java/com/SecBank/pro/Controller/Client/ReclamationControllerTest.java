package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.ReclamationController;
import com.SecBank.pro.Models.Admin.AgentSupport;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Cliens.Beneficiaire;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Cliens.ResponseReclamation;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Services.Clients.ReclamationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

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

    /**
     * Regression : avec un graphe complet (client, comptes, beneficiaires, ticket,
     * reponses), la serialisation bouclait et le corps de la reponse etait perdu
     * ("Document nesting depth (501) exceeds the maximum allowed (500)").
     */
    @Test
    @DisplayName("Given une reclamation reliee a son client, ses comptes et son ticket, Then le JSON reste serialisable")
    void serialisationAvecGrapheCompletNeBouclePas() throws Exception {
        Client client = new Client();
        client.setEmail("client.demo1@demo.bankati.test");
        client.setNom("Client Demo 1");

        CompteBancaire compte = new CompteBancaire();
        compte.setId(2001L);
        compte.setNumeroCompte("1000000000001");
        compte.setClient(client);
        client.setCompteBancaires(List.of(compte));

        Beneficiaire beneficiaire = new Beneficiaire();
        beneficiaire.setNom("Beneficiaire de test");
        beneficiaire.setClient(client);
        client.setBeneficiaires(List.of(beneficiaire));

        AgentSupport agent = new AgentSupport();
        agent.setNom("Agent Support Demo 1");

        Reclamation reclamation = new Reclamation();
        reclamation.setId(4001L);
        reclamation.setObjet("Frais bancaires inattendus");
        reclamation.setStatus(StatusReclamation.EN_COURS);
        reclamation.setClient(client);
        reclamation.setAgentsupp(agent);

        TicketSupport ticket = new TicketSupport();
        ticket.setId(5001L);
        ticket.setStatus(StatusTicket.EN_TRAITEMENT);
        ticket.setReclamation(reclamation);
        reclamation.setTicketSupport(ticket);

        ResponseReclamation reponse = new ResponseReclamation();
        reponse.setId(6001L);
        reponse.setContenu("Bonjour, nous traitons votre demande.");
        reponse.setReclamation(reclamation);
        reclamation.setResponseReclamations(List.of(reponse));

        when(reclamationService.suivreReclamation(4001L)).thenReturn(reclamation);

        mockMvc.perform(get("/api/reclamations/4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objet").value("Frais bancaires inattendus"))
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.client.nom").value("Client Demo 1"))
                .andExpect(jsonPath("$.ticketSupport.status").value("EN_TRAITEMENT"))
                .andExpect(jsonPath("$.responseReclamations[0].contenu")
                        .value("Bonjour, nous traitons votre demande."))
                .andExpect(jsonPath("$.client.compteBancaires").doesNotExist());
    }
}