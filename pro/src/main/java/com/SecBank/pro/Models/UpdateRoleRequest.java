package com.SecBank.pro.Models;


import com.SecBank.pro.Models.Enums.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** actif = true : activation ; actif = false : suspension. */
public record UpdateRoleRequest(@NotNull Role role) {
}

