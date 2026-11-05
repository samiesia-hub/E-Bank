package com.SecBank.pro.Services.Admin;

import java.util.List;

import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Enums.StatusTicket;

public interface TicketService {
    List<TicketSupport> consulterTickets(Long agentId);

    void mettreAJourStatutTicket(
        Long ticketId,
        StatusTicket statut
    );
}