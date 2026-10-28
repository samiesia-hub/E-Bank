package com.SecBank.pro.Services.Clients;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CompteServiceImpl implements CompteService {

    @Override
    public BigDecimal consulterSolde(Long compteId) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public List<CompteBancaire> consulterComptes(Long clientId) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void debiter(Long compteId, BigDecimal montant) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void crediter(Long compteId, BigDecimal montant) {
        throw new UnsupportedOperationException("Non implémenté");
    }

    @Override
    public void gererComptesBancaires() {
        throw new UnsupportedOperationException("Non implémenté");
    }
}