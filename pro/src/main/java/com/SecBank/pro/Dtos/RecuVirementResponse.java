package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecuVirementResponse(
        String reference,
        BigDecimal montant,
        String compteSource,
        String compteDestinataire,
        String beneficiaire,
        String motif,
        String statut,
        LocalDateTime dateTransaction
) {
    public static RecuVirementResponse depuis(Virement virement) {
        CompteBancaire source = virement.getCompte();
        CompteBancaire destination = virement.getCompteDestinataire();

        return new RecuVirementResponse(
                virement.getReference(),
                virement.getMontant(),
                source.getNumeroCompte(),
                destination.getNumeroCompte(),
                virement.getBeneficiaire(),
                virement.getMotif(),
                virement.getStatutTransaction().name(),
                virement.getDateTransaction()
        );
    }
}