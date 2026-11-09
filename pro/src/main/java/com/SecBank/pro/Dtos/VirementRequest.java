package com.SecBank.pro.Dtos;

import java.math.BigDecimal;

public record VirementRequest(
        Long compteSourceId,
        Long beneficiaireId,
        BigDecimal montant,
        String motif
) {
}