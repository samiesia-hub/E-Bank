package com.SecBank.pro.Dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Donnees consolidees du tableau de bord administrateur.
 *
 * Contrat attendu par le front (src/types/admin-dashboard.ts) :
 * GET /api/admin/dashboard
 */
public record DashboardAdminResponse(
        long utilisateursActifs,
        long ticketsOuverts,
        long dossiersKycEnAttente,
        List<ActiviteRecente> activitesRecentes
) {
    /** Une ligne du journal d'audit, sans exposer l'entite ni l'utilisateur complet. */
    public record ActiviteRecente(
            Long id,
            String action,
            String auteur,
            LocalDateTime date
    ) {}
}
