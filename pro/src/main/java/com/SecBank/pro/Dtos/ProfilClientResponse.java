package com.SecBank.pro.Dtos;

import com.SecBank.pro.Models.Cliens.Client;

public record ProfilClientResponse(
        Long id,
        String numeroClient,
        String nom,
        String email,
        String telephone,
        String adresse
) {
    public static ProfilClientResponse depuis(Client client) {
        return new ProfilClientResponse(
                client.getId_Utilisateur(),
                client.getNumeroClient(),
                client.getNom(),
                client.getEmail(),
                client.getTelephone() == 0 ? null : String.valueOf(client.getTelephone()),
                client.getAddress()
        );
    }
}