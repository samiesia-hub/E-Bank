package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VirementResponse(
        Long transactionId,
        String reference,
        Long compteSourceId,
        BigDecimal montant,
        String beneficiaire,
        String motif,
        String statut,
        LocalDateTime dateTransaction
) {
    public static VirementResponse depuis(Virement virement) {
        return new VirementResponse(
                virement.getId(),
                virement.getReference(),
                virement.getCompte().getId(),
                virement.getMontant(),
                virement.getBeneficiaire(),
                virement.getMotif(),
                virement.getStatutTransaction().name(),
                virement.getDateTransaction()
        );
    }
}