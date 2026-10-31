package com.SecBank.pro.Services;

import com.SecBank.pro.Models.Cliens.Virement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class VirementServiceImpl implements VirementService {

    @Override
    public Virement effectuerVirement(Long beneficiaireId, BigDecimal montant, String motif) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void executer(Long virementId) {
        throw new UnsupportedOperationException("Non implémenté");
    }
}