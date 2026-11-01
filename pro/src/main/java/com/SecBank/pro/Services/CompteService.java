package com.SecBank.pro.Services;

import com.SecBank.pro.Models.Cliens.CompteBancaire;

import java.math.BigDecimal;
import java.util.List;

public interface CompteService {

    BigDecimal consulterSolde(Long compteId);
    List<CompteBancaire> consulterComptes(Long clientId);

    void debiter(Long compteId, BigDecimal montant);

    void crediter(Long compteId, BigDecimal montant);

    void gererComptesBancaires();

}
