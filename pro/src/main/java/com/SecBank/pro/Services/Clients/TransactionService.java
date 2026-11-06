package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    List<Transaction> consulterHistorique(Long compteId);

    List<Transaction> consulterTransactions();

    /** Historique pagine et filtre d'un compte ; proprietaire verifie (403 sinon). */
    Page<Transaction> rechercherTransactionsDuCompte(Long compteId, String email,
                                                     LocalDate du, LocalDate au,
                                                     SensTransaction sens, Pageable pageable);

    /** Transactions des comptes du client connecte, avec compteId optionnel. */
    Page<Transaction> rechercherTransactionsDuClient(String email, Long compteId,
                                                     LocalDate du, LocalDate au,
                                                     SensTransaction sens, Pageable pageable);

    /** Supervision administrateur : toutes les transactions de la banque. */
    Page<Transaction> rechercherToutesLesTransactions(Long clientId, Long compteId,
                                                      LocalDate du, LocalDate au,
                                                      SensTransaction sens, Pageable pageable);
}
