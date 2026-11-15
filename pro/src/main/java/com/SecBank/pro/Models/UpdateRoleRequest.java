package com.SecBank.pro.Models;

import com.SecBank.pro.Models.Enums.Role;
import jakarta.validation.constraints.NotNull;

/** Nouveau role demande par un administrateur (SCRUM-97). */
public record UpdateRoleRequest(@NotNull Role role) {
}
