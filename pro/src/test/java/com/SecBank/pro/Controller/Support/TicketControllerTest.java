package com.SecBank.pro.Controller.Support;

import com.SecBank.pro.Conrollers.Support.TicketController;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Cliens.ResponseReclamation;
import com.SecBank.pro.Models.Enums.Priorite;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Services.Admin.TicketService;
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
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("Given un client connecte, When il ouvre un ticket, Then 201 avec le statut OUVERT")
    void creerTicket() throws Exception {
        when(ticketService.creerTicket("jean@example.com", "Virement absent", "Rien recu", null))
                .thenReturn(ticket(StatusTicket.OUVERT));

        mockMvc.perform(post("/api/client/tickets")
                        .principal(principal("jean@example.com", "ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objet\":\"Virement absent\",\"description\":\"Rien recu\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andExpect(jsonPath("$.priorite").value("MOYENNE"))
                .andExpect(jsonPath("$.clientEmail").value("jean@example.com"))
                .andExpect(jsonPath("$.reponses[0].contenu").value("Nous traitons votre demande."));
    }

    @Test
    @DisplayName("Given un client connecte, When il liste ses tickets, Then seuls ses tickets reviennent")
    void mesTickets() throws Exception {
        when(ticketService.consulterTicketsDuClient("jean@example.com")).thenReturn(List.of(ticket(StatusTicket.OUVERT)));

        mockMvc.perform(get("/api/client/tickets").principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objet").value("Virement absent"));
    }

    @Test
    @DisplayName("Given un agent support, When il consulte la file, Then tous les tickets reviennent")
    void fileDeTickets() throws Exception {
        when(ticketService.consulterTickets()).thenReturn(List.of(ticket(StatusTicket.OUVERT), ticket(StatusTicket.FERME)));

        mockMvc.perform(get("/api/support/tickets").principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Given un agent, When il change le statut, Then le ticket mis a jour est renvoye")
    void changerStatut() throws Exception {
        when(ticketService.mettreAJourStatutTicket(eq(5L), eq(StatusTicket.FERME))).thenReturn(ticket(StatusTicket.FERME));

        mockMvc.perform(patch("/api/support/tickets/5/status")
                        .principal(principal("agent@example.com", "ROLE_AGENT_SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"FERME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FERME"));
    }

    private static TicketSupport ticket(StatusTicket statut) {
        Client client = new Client();
        client.setEmail("jean@example.com");

        Reclamation reclamation = new Reclamation();
        reclamation.setId(9L);
        reclamation.setObjet("Virement absent");
        reclamation.setDesciprtion("Rien recu");
        reclamation.setClient(client);

        ResponseReclamation reponse = new ResponseReclamation();
        reponse.setContenu("Nous traitons votre demande.");
        reponse.setDatResponse(new Date());
        reclamation.setResponseReclamations(List.of(reponse));

        TicketSupport ticket = new TicketSupport();
        ticket.setId(5L);
        ticket.setStatus(statut);
        ticket.setPriorite(Priorite.MOYENNE);
        ticket.setDateOuverture(new Date());
        ticket.setReclamation(reclamation);
        return ticket;
    }

    private static Principal principal(String email, String role) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
