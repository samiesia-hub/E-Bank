package com.SecBank.pro.Dtos;

import java.math.BigDecimal;

/**
 * Demande de virement.
 *
 * <p>Le destinataire peut etre designe de deux facons :</p>
 * <ul>
 *   <li>{@code beneficiaireId} : un beneficiaire enregistre par le client ;</li>
 *   <li>{@code numeroCompteBeneficiaire} : le RIB (24 chiffres), le numero de
 *       compte (13 chiffres) ou l'IBAN du compte a crediter, sans passer par un
 *       beneficiaire enregistre.</li>
 * </ul>
 */
public record VirementRequest(
        Long compteSourceId,
        Long beneficiaireId,
        String numeroCompteBeneficiaire,
        BigDecimal montant,
        String motif
) {
}
