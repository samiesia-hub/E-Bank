package com.SecBank.pro.Dtos;

// Corps de PUT /api/client/profile
public record ModifierProfilRequest(String nom, String telephone, String adresse) {
}