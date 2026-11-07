package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Beneficiaire;

import java.util.List;

public interface BeneficiaireService {

    List<Beneficiaire> listerBeneficiaires(String emailClient);

    Beneficiaire creerBeneficiaire(
            String emailClient,
            String nom,
            String prenom,
            String numeroCompte
    );

    void supprimerBeneficiaire(
            String emailClient,
            Long beneficiaireId
    );
}