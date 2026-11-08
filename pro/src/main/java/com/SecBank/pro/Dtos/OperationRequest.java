package com.SecBank.pro.Dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Corps d'un depot / retrait au guichet.
 * Le montant est en BigDecimal : jamais de double pour manipuler de l'argent.
 */
@Schema(name = "OperationRequest", description = "Corps d'un depot ou d'un retrait au guichet")
public record OperationRequest(
        @Schema(description = "Montant en MAD, strictement positif", example = "250.00") BigDecimal montant,
        @Schema(description = "Motif de l'operation", example = "Depot au guichet") String motif) {
}
