package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Admin.AgentSupport;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.Priorite;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Repositories.Admin.SupporRepo;
import com.SecBank.pro.Repositories.Admin.TicketSupportRepo;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Implementation des tickets support (SCRUM-107).
 *
 * Les statuts du domaine sont en francais : OUVERT, EN_TRAITEMENT,
 * EN_ATTENTE_CLIENT et FERME (voir StatusTicket).
 */
@Service
@AllArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketSupportRepo ticketSupportRepo;
    private final ReclamationRepo reclamationRepo;
    private final ClientRepo clientRepo;
    private final SupporRepo supporRepo;

    @Override
    @Transactional
    public TicketSupport creerTicket(String emailClient, String objet, String description, Priorite priorite) {
        Client client = clientRepo.findByEmail(emailClient)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec email :" + emailClient));

        if (objet == null || objet.isBlank()) {
            throw new DonneeInvalideException("L'objet du ticket est obligatoire");
        }
        if (description == null || description.isBlank()) {
            throw new DonneeInvalideException("La description du ticket est obligatoire");
        }

        Reclamation reclamation = new Reclamation();
        reclamation.setObjet(objet);
        reclamation.setDesciprtion(description);
        reclamation.setDateCreation(new Date());
        reclamation.setStatus(StatusReclamation.OUVERTE);
        reclamation.setClient(client);
        reclamationRepo.save(reclamation);

        TicketSupport ticket = new TicketSupport();
        ticket.setStatus(StatusTicket.OUVERT);
        ticket.setPriorite(priorite == null ? Priorite.MOYENNE : priorite);
        ticket.setDateOuverture(new Date());
        ticket.setReclamation(reclamation);

        return ticketSupportRepo.save(ticket);
    }

    @Override
    public List<TicketSupport> consulterTicketsDuClient(String emailClient) {
        return ticketSupportRepo.findByReclamationClientEmailOrderByDateOuvertureDesc(emailClient);
    }

    @Override
    public List<TicketSupport> consulterTickets() {
        return ticketSupportRepo.findAllByOrderByDateOuvertureDesc();
    }

    @Override
    @Transactional
    public TicketSupport assignerTicket(Long ticketId, Long agentId) {
        TicketSupport ticket = trouverTicket(ticketId);
        AgentSupport agent = supporRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent support introuvable avec id :" + agentId));

        ticket.setAgentsupp(agent);
        if (ticket.getStatus() == StatusTicket.OUVERT) {
            ticket.setStatus(StatusTicket.EN_TRAITEMENT);
        }

        return ticketSupportRepo.save(ticket);
    }

    @Override
    @Transactional
    public TicketSupport mettreAJourStatutTicket(Long ticketId, StatusTicket statut) {
        if (statut == null) {
            throw new DonneeInvalideException("Le statut du ticket est obligatoire");
        }

        TicketSupport ticket = trouverTicket(ticketId);
        ticket.setStatus(statut);
        if (statut == StatusTicket.FERME) {
            ticket.setDateFermetur(new Date());
        }

        return ticketSupportRepo.save(ticket);
    }

    private TicketSupport trouverTicket(Long ticketId) {
        return ticketSupportRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable avec id :" + ticketId));
    }
}
