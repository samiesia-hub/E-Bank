package com.SecBank.pro.Dtos.Admin;




import com.SecBank.pro.Models.Enums.Role;

import com.SecBank.pro.Models.Utilisateur;

import java.time.LocalDateTime;

/** Vue administrateur d'un utilisateur. Ne contient jamais le hash du mot de passe. */
public record AdminUserDTO(
        Long id,
        String email,
        String nom,
        Role role,
        boolean actif,
        LocalDateTime dateCreation) {

    public static AdminUserDTO depuis(Utilisateur u) {
        return new AdminUserDTO(
                u.getId_Utilisateur(),
                u.getEmail(),
                u.getNom(),
                u.getRole(),
                u.isActif(),
                u.getDateCreation());
    }
}