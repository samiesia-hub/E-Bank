package Service.Admin;
import java.util.List;
public interface TicketService{
    List<TicketSupport> consulterTickets(Long agentId);
    void mettreAJourStatutTicket(
        Long ticketId,
        StatutTicket statut
    );
}