package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.Beneficiaire;

public record BeneficiaireResponse(
        Long id,
        String nom,
        String prenom,
        String numeroCompte
) {
    public static BeneficiaireResponse depuis(Beneficiaire beneficiaire) {
        return new BeneficiaireResponse(
                beneficiaire.getId(),
                beneficiaire.getNom(),
                beneficiaire.getPrenom(),
                beneficiaire.getNumeroCompte()
        );
    }
}