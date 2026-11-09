package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Cliens.ResponseReclamation;

import java.util.Date;
import java.util.List;

/**
 * Vue d'un ticket pour l'API : le ticket seul ne porte pas l'objet ni
 * l'historique (ils vivent dans la reclamation liee), et la serialisation de
 * l'entite exposerait des references circulaires.
 */
public record TicketResponse(
        Long id,
        String objet,
        String description,
        String statut,
        String priorite,
        Date dateOuverture,
        Date dateFermeture,
        String clientEmail,
        String agentEmail,
        List<Reponse> reponses
) {

    public record Reponse(String contenu, Date date) {
    }

    public static TicketResponse depuis(TicketSupport ticket) {
        Reclamation reclamation = ticket.getReclamation();

        return new TicketResponse(
                ticket.getId(),
                reclamation == null ? null : reclamation.getObjet(),
                reclamation == null ? null : reclamation.getDesciprtion(),
                ticket.getStatus() == null ? null : ticket.getStatus().name(),
                ticket.getPriorite() == null ? null : ticket.getPriorite().name(),
                ticket.getDateOuverture(),
                ticket.getDateFermetur(),
                reclamation == null || reclamation.getClient() == null
                        ? null : reclamation.getClient().getEmail(),
                ticket.getAgentsupp() == null ? null : ticket.getAgentsupp().getEmail(),
                versReponses(reclamation)
        );
    }

    private static List<Reponse> versReponses(Reclamation reclamation) {
        if (reclamation == null || reclamation.getResponseReclamations() == null) {
            return List.of();
        }
        return reclamation.getResponseReclamations().stream()
                .map(TicketResponse::versReponse)
                .toList();
    }

    private static Reponse versReponse(ResponseReclamation reponse) {
        return new Reponse(reponse.getContenu(), reponse.getDatResponse());
    }
}
