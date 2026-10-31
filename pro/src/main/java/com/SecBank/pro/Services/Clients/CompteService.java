package com.SecBank.pro.Services.Clients;

import java.math.BigDecimal;
import java.util.List;

import com.SecBank.pro.Models.Cliens.CompteBancaire;

public interface CompteService {

    BigDecimal consulterSolde(Long compteId);

    List<CompteBancaire> consulterComptes(Long clientId);

    void debiter(Long compteId, BigDecimal montant);

    void crediter(Long compteId, BigDecimal montant);

    void gererComptesBancaires();
}