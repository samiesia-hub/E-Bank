package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.CompteBancaire;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue REST d'un compte bancaire.
 * Un DTO evite la boucle JSON client -> comptes -> client et expose le solde en BigDecimal.
 */
public record CompteResponse(
        Long id,
        String numeroCompte,
        String rib,
        String devise,
        BigDecimal solde,
        String typeCompte,
        String statutCompte,
        LocalDateTime dateCreation,
        Long clientId,
        String clientEmail,
        String clientNom
) {
    public static CompteResponse depuis(CompteBancaire compte) {
        return new CompteResponse(
                compte.getId(),
                compte.getNumeroCompte(),
                compte.getRib(),
                compte.getDevise(),
                compte.getSolde(),
                compte.getTypeCompte() == null ? null : compte.getTypeCompte().name(),
                compte.getStatutCompte() == null ? null : compte.getStatutCompte().name(),
                compte.getDateCreation(),
                compte.getClient() == null ? null : compte.getClient().getId_Utilisateur(),
                compte.getClient() == null ? null : compte.getClient().getEmail(),
                compte.getClient() == null ? null : compte.getClient().getNom()
        );
    }
}
