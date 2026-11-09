package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Enums.Priorite;
import com.SecBank.pro.Models.Enums.StatusTicket;

import java.util.List;

/**
 * Gestion des tickets support (SCRUM-107).
 *
 * Un ticket prolonge la reclamation du client : la reclamation porte l'objet,
 * la description et l'historique des echanges, le ticket porte le statut, la
 * priorite et l'agent qui le traite.
 */
public interface TicketService {

    /** Cree un ticket (statut OUVERT) rattache a la reclamation du client connecte. */
    TicketSupport creerTicket(String emailClient, String objet, String description, Priorite priorite);

    /** Tickets du client connecte, du plus recent au plus ancien. */
    List<TicketSupport> consulterTicketsDuClient(String emailClient);

    /** File de tickets vue par le support, du plus recent au plus ancien. */
    List<TicketSupport> consulterTickets();

    /** Assigne le ticket a un agent support et le passe en EN_TRAITEMENT. */
    TicketSupport assignerTicket(Long ticketId, Long agentId);

    /** Change le statut du ticket ; la fermeture renseigne la date de fermeture. */
    TicketSupport mettreAJourStatutTicket(Long ticketId, StatusTicket statut);
}
