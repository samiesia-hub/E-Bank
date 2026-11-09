package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VirementService {

    Virement effectuerVirement(
            String emailClient,
            Long compteSourceId,
            Long beneficiaireId,
            BigDecimal montant,
            String motif
    );

    Virement consulterRecu(String emailClient, String reference);

    Page<Virement> consulterHistorique(
            String emailClient,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutTransaction statut,
            Pageable pageable
    );
}