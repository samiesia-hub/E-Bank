package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    /**
     * Somme des transactions d'un sens et d'un statut sur un intervalle [debut, fin).
     * Renvoie null si aucune ligne : l'appelant remplace par zero.
     */
    @Query("SELECT SUM(t.montant) FROM Transaction t "
            + "WHERE t.sens = :sens AND t.statutTransaction = :statut "
            + "AND t.dateTransaction >= :debut AND t.dateTransaction < :fin")
    BigDecimal sommeMontant(@Param("sens") SensTransaction sens,
                            @Param("statut") StatutTransaction statut,
                            @Param("debut") LocalDateTime debut,
                            @Param("fin") LocalDateTime fin);

    /** Nombre de transactions sur un intervalle [debut, fin). */
    @Query("SELECT COUNT(t) FROM Transaction t "
            + "WHERE t.dateTransaction >= :debut AND t.dateTransaction < :fin")
    long compterSurPeriode(@Param("debut") LocalDateTime debut,
                           @Param("fin") LocalDateTime fin);

    /** Nombre de transactions d'un statut donne sur un intervalle [debut, fin). */
    @Query("SELECT COUNT(t) FROM Transaction t "
            + "WHERE t.statutTransaction = :statut "
            + "AND t.dateTransaction >= :debut AND t.dateTransaction < :fin")
    long compterStatutSurPeriode(@Param("statut") StatutTransaction statut,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);
}
