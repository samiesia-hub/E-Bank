package com.SecBank.pro.Dtos;

public record CreationBeneficiaireRequest(
        String nom,
        String prenom,
        String numeroCompte
) {
}