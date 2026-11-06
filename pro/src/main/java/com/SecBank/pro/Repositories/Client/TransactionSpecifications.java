package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Filtres reutilisables pour la recherche paginee des transactions.
 * Un filtre null est neutre (conjonction vide).
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> parCompte(Long compteId) {
        return (root, query, cb) -> compteId == null
                ? cb.conjunction()
                : cb.equal(root.get("compte").get("id"), compteId);
    }

    public static Specification<Transaction> parClient(Long clientId) {
        return (root, query, cb) -> clientId == null
                ? cb.conjunction()
                : cb.equal(root.get("compte").get("client").get("id_Utilisateur"), clientId);
    }

    /** Periode inclusive sur les jours : [du 00:00, au lendemain 00:00[. */
    public static Specification<Transaction> entreDates(LocalDate du, LocalDate au) {
        return (root, query, cb) -> {
            Predicate predicat = cb.conjunction();
            if (du != null) {
                Predicate apresDebut = cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("dateTransaction"), du.atStartOfDay());
                predicat = cb.and(predicat, apresDebut);
            }
            if (au != null) {
                Predicate avantFin = cb.lessThan(
                        root.<LocalDateTime>get("dateTransaction"), au.plusDays(1).atStartOfDay());
                predicat = cb.and(predicat, avantFin);
            }
            return predicat;
        };
    }

    public static Specification<Transaction> parSens(SensTransaction sens) {
        return (root, query, cb) -> sens == null
                ? cb.conjunction()
                : cb.equal(root.get("sens"), sens);
    }
}
