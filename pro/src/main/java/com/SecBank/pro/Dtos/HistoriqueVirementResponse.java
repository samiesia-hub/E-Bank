package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoriqueVirementResponse(
        String reference,
        LocalDateTime dateTransaction,
        BigDecimal montant,
        String compteSource,
        String compteDestinataire,
        String statut,
        String beneficiaire,
        String motif
) {
    public static HistoriqueVirementResponse depuis(Virement virement) {
        CompteBancaire source = virement.getCompte();
        CompteBancaire destination = virement.getCompteDestinataire();

        return new HistoriqueVirementResponse(
                virement.getReference(),
                virement.getDateTransaction(),
                virement.getMontant(),
                source.getNumeroCompte(),
                destination.getNumeroCompte(),
                virement.getStatutTransaction().name(),
                virement.getBeneficiaire(),
                virement.getMotif()
        );
    }
}