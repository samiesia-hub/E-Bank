package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.Reclamation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Vue REST d'une réclamation : objet, message, statut, date de création
 * et date de dernière mise à jour. Pas d'entité exposée, donc pas de boucle JSON.
 */
public record ReclamationResponse(
        Long id,
        String objet,
        String message,
        String statut,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
    public static ReclamationResponse depuis(Reclamation reclamation) {
        // Anciennes lignes sans date de mise à jour : on retombe sur la date de création.
        Date miseAJour = reclamation.getDateMiseAJour() != null
                ? reclamation.getDateMiseAJour()
                : reclamation.getDateCreation();
        return new ReclamationResponse(
                reclamation.getId(),
                reclamation.getObjet(),
                reclamation.getDesciprtion(),
                reclamation.getStatus() == null ? null : reclamation.getStatus().name(),
                enLocalDateTime(reclamation.getDateCreation()),
                enLocalDateTime(miseAJour)
        );
    }

    private static LocalDateTime enLocalDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}