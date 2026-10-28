package com.SecBank.pro.Services.Clients;

import java.math.BigDecimal;

public interface CompteService {

    BigDecimal consulterSolde(Long compteId);
    List<CompteBancaire> consulterComptes(Long clientId);

    void debiter(Long compteId, BigDecimal montant);

    void crediter(Long compteId, BigDecimal montant);

    void gererComptesBancaires();

}
