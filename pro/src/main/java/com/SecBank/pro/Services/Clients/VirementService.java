package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Virement;

import java.math.BigDecimal;

public interface VirementService {

    Virement effectuerVirement(Long beneficiaireId, BigDecimal montant, String motif);

    void executer(Long virementId);
}