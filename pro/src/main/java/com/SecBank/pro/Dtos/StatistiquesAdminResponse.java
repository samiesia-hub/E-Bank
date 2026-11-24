package com.SecBank.pro.Dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indicateurs cles de la vue d'ensemble administrateur.
 *
 * Contrat attendu par le front (src/features/admin/types.ts) :
 * GET /api/admin/statistiques?periode=aujourdhui|7j|30j
 *
 * Les tendances sont la variation en pourcentage par rapport a la periode
 * precedente de meme duree (ex. 3.2 ou -1.5).
 */
public record StatistiquesAdminResponse(
        long totalClients,
        Montant totalDepots,
        long transactionsDuJour,
        long alertesFraude,
        Tendances tendances
) {
    /** Montant au format attendu par le front : valeur et devise separees, sans flottant. */
    public record Montant(String montant, String devise) {
        public static Montant de(BigDecimal valeur, String devise) {
            BigDecimal sur = valeur == null ? BigDecimal.ZERO : valeur.setScale(2, RoundingMode.HALF_UP);
            return new Montant(sur.toPlainString(), devise);
        }
    }

    public record Tendances(
            double totalClients,
            double totalDepots,
            double transactionsDuJour,
            double alertesFraude
    ) {}

    /** Variation en pourcentage entre deux valeurs entieres, arrondie a une decimale. */
    public static double variation(long precedent, long courant) {
        return variation(BigDecimal.valueOf(precedent), BigDecimal.valueOf(courant));
    }

    /** Variation en pourcentage entre deux montants, arrondie a une decimale. */
    public static double variation(BigDecimal precedent, BigDecimal courant) {
        BigDecimal avant = precedent == null ? BigDecimal.ZERO : precedent;
        BigDecimal apres = courant == null ? BigDecimal.ZERO : courant;
        if (avant.signum() == 0) {
            return apres.signum() == 0 ? 0.0 : 100.0;
        }
        double pourcentage = apres.subtract(avant)
                .multiply(BigDecimal.valueOf(100))
                .divide(avant, 4, RoundingMode.HALF_UP)
                .doubleValue();
        return Math.round(pourcentage * 10.0) / 10.0;
    }
}
