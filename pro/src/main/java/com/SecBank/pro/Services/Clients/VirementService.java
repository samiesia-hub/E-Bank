package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;

public interface VirementService {

    Virement effectuerVirement(
            String emailClient,
            Long compteSourceId,
            Long beneficiaireId,
            BigDecimal montant,
            String motif
    );

    Virement consulterRecu(String emailClient, String reference);
}