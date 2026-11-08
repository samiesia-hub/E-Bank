package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VirementRepository extends JpaRepository<Virement, Long> {

    Optional<Virement> findByReference(String reference);

    @Query("""
    SELECT v FROM Virement v
    WHERE v.compte.client.email = :emailClient
      AND v.sens = :sens
      AND v.reference IS NOT NULL
      AND v.dateTransaction >= :dateDebut
      AND v.dateTransaction < :dateFinExclusive
      AND v.statutTransaction = COALESCE(:statut, v.statutTransaction)
    """)
Page<Virement> rechercherHistoriqueDuClient(
        @Param("emailClient") String emailClient,
        @Param("sens") SensTransaction sens,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFinExclusive") LocalDateTime dateFinExclusive,
        @Param("statut") StatutTransaction statut,
        Pageable pageable
);
}