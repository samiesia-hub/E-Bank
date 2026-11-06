package com.SecBank.pro.Dtos;

import java.math.BigDecimal;

/**
 * Corps d'un depot / retrait au guichet.
 * Le montant est en BigDecimal : jamais de double pour manipuler de l'argent.
 */
public record OperationRequest(BigDecimal montant, String motif) {
}
