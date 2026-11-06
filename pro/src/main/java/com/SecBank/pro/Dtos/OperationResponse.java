package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.OperationGuichet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resultat d'une operation de guichet : transaction creee et solde resultant. */
public record OperationResponse(
        Long transactionId,
        String typeOperation,
        BigDecimal montant,
        BigDecimal nouveauSolde,
        Long compteId,
        String numeroCompte,
        LocalDateTime dateOperation,
        String motif,
        String agent
) {
    public static OperationResponse depuis(OperationGuichet operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getTypeOperation() == null ? null : operation.getTypeOperation().name(),
                operation.getMontant(),
                operation.getCompte() == null ? null : operation.getCompte().getSolde(),
                operation.getCompte() == null ? null : operation.getCompte().getId(),
                operation.getCompte() == null ? null : operation.getCompte().getNumeroCompte(),
                operation.getDateTransaction(),
                operation.getMotif(),
                operation.getAgent() == null ? null : operation.getAgent().getEmail()
        );
    }
}
