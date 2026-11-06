package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue REST d'une transaction : montant en BigDecimal, jamais de double.
 */
public record TransactionResponse(
        Long id,
        LocalDateTime dateTransaction,
        BigDecimal montant,
        String sens,
        String statutTransaction,
        String libelle,
        Long compteId,
        String numeroCompte,
        Long clientId
) {
    public static TransactionResponse depuis(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDateTransaction(),
                transaction.getMontant(),
                transaction.getSens() == null ? null : transaction.getSens().name(),
                transaction.getStatutTransaction() == null ? null : transaction.getStatutTransaction().name(),
                libelle(transaction),
                transaction.getCompte() == null ? null : transaction.getCompte().getId(),
                transaction.getCompte() == null ? null : transaction.getCompte().getNumeroCompte(),
                transaction.getCompte() == null || transaction.getCompte().getClient() == null
                        ? null
                        : transaction.getCompte().getClient().getId_Utilisateur()
        );
    }

    private static String libelle(Transaction transaction) {
        if (transaction instanceof Virement virement) {
            return virement.getMotif() != null ? virement.getMotif() : virement.getBeneficiaire();
        }
        if (transaction instanceof OperationGuichet operation) {
            return operation.getMotif();
        }
        return null;
    }
}
