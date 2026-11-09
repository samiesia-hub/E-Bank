package com.SecBank.pro.Dtos;

// Corps de POST /api/auth/reset-password
public record ResetPasswordDTO(String email, String code, String nouveauMotDePasse) {
}
