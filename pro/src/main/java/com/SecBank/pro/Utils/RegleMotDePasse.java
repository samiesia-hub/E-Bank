package com.SecBank.pro.Utils;

import com.SecBank.pro.Exceptions.DonneeInvalideException;

import java.nio.charset.StandardCharsets;

/**
 * Règle de mot de passe commune, identique au formulaire d'inscription du frontend :
 * 12 caractères minimum, une majuscule, une minuscule, un chiffre,
 * et 72 octets maximum (limite de BCrypt).
 */
public final class RegleMotDePasse {

    private RegleMotDePasse() {
    }

    public static void verifier(String motDePasse) {
        boolean conforme = motDePasse != null
                && motDePasse.length() >= 12
                && motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72
                && motDePasse.chars().anyMatch(Character::isUpperCase)
                && motDePasse.chars().anyMatch(Character::isLowerCase)
                && motDePasse.chars().anyMatch(Character::isDigit);
        if (!conforme) {
            throw new DonneeInvalideException(
                    "Le nouveau mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule et un chiffre");
        }
    }
}
