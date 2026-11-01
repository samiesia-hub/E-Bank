package com.SecBank.pro.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardClientResponse(
    List<CompteResume> comptes,
    List<VirementResume> derniersVirements,
    long nombreReclamationsOuvertes
) {
    public record CompteResume(
        Long id,
        String numeroCompte,
        String typeCompte,
        BigDecimal solde,
        String statutCompte
    ) {}

    public record VirementResume(
        Long id,
        LocalDateTime dateTransaction,
        BigDecimal montant,
        String beneficiaire,
        String motif,
        String statut
    ) {}
}