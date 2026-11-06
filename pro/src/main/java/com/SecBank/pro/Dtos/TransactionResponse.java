package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.TypeOperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue REST d'une transaction : montant en BigDecimal, jamais de double.
 * Emetteur / destinataire sont deduits du type de transaction pour que la
 * supervision admin et l'export CSV exposent les memes colonnes.
 */
public record TransactionResponse(
        Long id,
        LocalDateTime dateTransaction,
        BigDecimal montant,
        String devise,
        String sens,
        String statutTransaction,
        String libelle,
        Long compteId,
        String numeroCompte,
        Long clientId,
        String emetteur,
        String destinataire
) {
    public static TransactionResponse depuis(Transaction transaction) {
        String titulaire = titulaire(transaction);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getDateTransaction(),
                transaction.getMontant(),
                transaction.getCompte() == null ? null : transaction.getCompte().getDevise(),
                transaction.getSens() == null ? null : transaction.getSens().name(),
                transaction.getStatutTransaction() == null ? null : transaction.getStatutTransaction().name(),
                libelle(transaction),
                transaction.getCompte() == null ? null : transaction.getCompte().getId(),
                transaction.getCompte() == null ? null : transaction.getCompte().getNumeroCompte(),
                transaction.getCompte() == null || transaction.getCompte().getClient() == null
                        ? null
                        : transaction.getCompte().getClient().getId_Utilisateur(),
                emetteur(transaction, titulaire),
                destinataire(transaction, titulaire)
        );
    }

    /** Titulaire du compte mouvemente, affiche comme nom ou, a defaut, email. */
    private static String titulaire(Transaction transaction) {
        if (transaction.getCompte() == null || transaction.getCompte().getClient() == null) {
            return null;
        }
        String nom = transaction.getCompte().getClient().getNom();
        return nom == null || nom.isBlank() ? transaction.getCompte().getClient().getEmail() : nom;
    }

    /** Contrepartie guichet : l'agence, avec l'agent auteur si connu. */
    private static String guichet(OperationGuichet operation) {
        String agent = operation.getAgent() == null ? null : operation.getAgent().getNom();
        return agent == null || agent.isBlank() ? "Guichet" : "Guichet - " + agent;
    }

    private static String emetteur(Transaction transaction, String titulaire) {
        if (transaction instanceof Virement virement) {
            return virement.getSens() == SensTransaction.CREDIT ? virement.getBeneficiaire() : titulaire;
        }
        if (transaction instanceof OperationGuichet operation) {
            return operation.getTypeOperation() == TypeOperation.DEPOT ? guichet(operation) : titulaire;
        }
        return titulaire;
    }

    private static String destinataire(Transaction transaction, String titulaire) {
        if (transaction instanceof Virement virement) {
            return virement.getSens() == SensTransaction.CREDIT ? titulaire : virement.getBeneficiaire();
        }
        if (transaction instanceof OperationGuichet operation) {
            return operation.getTypeOperation() == TypeOperation.DEPOT ? titulaire : guichet(operation);
        }
        return titulaire;
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
