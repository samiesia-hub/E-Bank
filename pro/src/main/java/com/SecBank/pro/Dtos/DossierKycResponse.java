package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.DossierKYC;

import java.time.LocalDateTime;

/**
 * Vue d'un dossier KYC pour l'API (pas de serialisation d'entite : les
 * relations client / administrateur se referencent mutuellement).
 */
public record DossierKycResponse(
        Long id,
        String statut,
        LocalDateTime dateSoumission,
        Long clientId,
        String clientEmail,
        String clientNom,
        String decideurEmail
) {

    public static DossierKycResponse depuis(DossierKYC dossier) {
        return new DossierKycResponse(
                dossier.getId(),
                dossier.getStatutKYC() == null ? null : dossier.getStatutKYC().name(),
                dossier.getDateSoumission(),
                dossier.getClient() == null ? null : dossier.getClient().getId_Utilisateur(),
                dossier.getClient() == null ? null : dossier.getClient().getEmail(),
                dossier.getClient() == null ? null : dossier.getClient().getNom(),
                dossier.getAdmin() == null ? null : dossier.getAdmin().getEmail()
        );
    }
}
