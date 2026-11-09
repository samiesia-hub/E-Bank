package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Admin.AgentSupport;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.Priorite;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Repositories.Admin.SupporRepo;
import com.SecBank.pro.Repositories.Admin.TicketSupportRepo;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketSupportRepo ticketSupportRepo;

    @Mock
    private ReclamationRepo reclamationRepo;

    @Mock
    private ClientRepo clientRepo;

    @Mock
    private SupporRepo supporRepo;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    @DisplayName("Given un client, When il ouvre un ticket, Then le ticket est OUVERT et rattache a sa reclamation")
    void creerTicketCreeUnTicketOuvert() {
        when(clientRepo.findByEmail("jean@example.com")).thenReturn(Optional.of(client()));
        when(reclamationRepo.save(any(Reclamation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketSupportRepo.save(any(TicketSupport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSupport ticket = ticketService.creerTicket("jean@example.com", "Virement absent", "Rien recu", null);

        assertEquals(StatusTicket.OUVERT, ticket.getStatus());
        assertEquals(Priorite.MOYENNE, ticket.getPriorite());
        assertNotNull(ticket.getDateOuverture());
        assertEquals("Virement absent", ticket.getReclamation().getObjet());
        assertEquals("jean@example.com", ticket.getReclamation().getClient().getEmail());
    }

    @Test
    @DisplayName("Given un objet vide, When le client ouvre un ticket, Then 400 et rien n'est enregistre")
    void creerTicketRefuseUnObjetVide() {
        when(clientRepo.findByEmail("jean@example.com")).thenReturn(Optional.of(client()));

        assertThrows(DonneeInvalideException.class,
                () -> ticketService.creerTicket("jean@example.com", "  ", "Rien recu", null));

        verify(ticketSupportRepo, never()).save(any());
        verify(reclamationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Given un email inconnu, When un ticket est cree, Then 404")
    void creerTicketRefuseUnClientInconnu() {
        when(clientRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.creerTicket("inconnu@example.com", "Objet", "Description", null));
    }

    @Test
    @DisplayName("Given un ticket OUVERT, When un agent l'assigne, Then l'agent est trace et le statut passe EN_TRAITEMENT")
    void assignerTicketRattacheUnAgent() {
        TicketSupport ticket = ticket(StatusTicket.OUVERT);
        AgentSupport agent = new AgentSupport();
        agent.setId_Utilisateur(7L);
        agent.setEmail("agent@example.com");

        when(ticketSupportRepo.findById(5L)).thenReturn(Optional.of(ticket));
        when(supporRepo.findById(7L)).thenReturn(Optional.of(agent));
        when(ticketSupportRepo.save(any(TicketSupport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSupport resultat = ticketService.assignerTicket(5L, 7L);

        assertEquals("agent@example.com", resultat.getAgentsupp().getEmail());
        assertEquals(StatusTicket.EN_TRAITEMENT, resultat.getStatus());
    }

    @Test
    @DisplayName("Given un ticket, When il est ferme, Then la date de fermeture est renseignee")
    void fermerTicketRenseigneLaDateDeFermeture() {
        TicketSupport ticket = ticket(StatusTicket.EN_TRAITEMENT);

        when(ticketSupportRepo.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketSupportRepo.save(any(TicketSupport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSupport resultat = ticketService.mettreAJourStatutTicket(5L, StatusTicket.FERME);

        assertEquals(StatusTicket.FERME, resultat.getStatus());
        assertNotNull(resultat.getDateFermetur());
    }

    @Test
    @DisplayName("Given un statut absent, When le statut est change, Then 400")
    void changerStatutRefuseUnStatutAbsent() {
        assertThrows(DonneeInvalideException.class,
                () -> ticketService.mettreAJourStatutTicket(5L, null));
    }

    @Test
    @DisplayName("Given un client, When il liste ses tickets, Then seuls ses tickets sont demandes")
    void listerLesTicketsDuClientUtiliseLEmail() {
        when(ticketSupportRepo.findByReclamationClientEmailOrderByDateOuvertureDesc("jean@example.com"))
                .thenReturn(List.of(ticket(StatusTicket.OUVERT)));

        List<TicketSupport> tickets = ticketService.consulterTicketsDuClient("jean@example.com");

        assertEquals(1, tickets.size());
        ArgumentCaptor<String> email = ArgumentCaptor.forClass(String.class);
        verify(ticketSupportRepo).findByReclamationClientEmailOrderByDateOuvertureDesc(email.capture());
        assertEquals("jean@example.com", email.getValue());
    }

    @Test
    @DisplayName("Given des tickets, When le support consulte la file, Then ils sont tries du plus recent au plus ancien")
    void fileDeTicketsUtiliseLeTriParDate() {
        when(ticketSupportRepo.findAllByOrderByDateOuvertureDesc())
                .thenReturn(List.of(ticket(StatusTicket.OUVERT), ticket(StatusTicket.FERME)));

        List<TicketSupport> tickets = ticketService.consulterTickets();

        assertEquals(2, tickets.size());
        verify(ticketSupportRepo).findAllByOrderByDateOuvertureDesc();
    }

    private static Client client() {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");
        client.setNom("Jean");
        return client;
    }

    private static TicketSupport ticket(StatusTicket statut) {
        TicketSupport ticket = new TicketSupport();
        ticket.setId(5L);
        ticket.setStatus(statut);
        ticket.setPriorite(Priorite.MOYENNE);
        ticket.setDateOuverture(new Date());
        assertTrue(ticket.getId() > 0);
        return ticket;
    }
}
