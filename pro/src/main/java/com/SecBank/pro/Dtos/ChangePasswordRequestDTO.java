package com.SecBank.pro.Dtos;

// Corps de POST /api/client/change-password
public record ChangePasswordRequestDTO(String motDePasseActuel, String nouveauMotDePasse) {
}