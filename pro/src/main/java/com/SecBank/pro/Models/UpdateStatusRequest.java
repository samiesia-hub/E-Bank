package com.SecBank.pro.Models;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** actif = true : activation ; actif = false : suspension. */
public record UpdateStatusRequest(
        @NotNull Boolean actif,
        @Size(max = 255) String motif) {
}