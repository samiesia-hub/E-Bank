package com.SecBank.pro.Services;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Codes OTP à usage unique, gardés en mémoire.
 * Suffisant pour ce sprint ; à remplacer par la table CodeOTP en production.
 */
@Service
public class OtpService {

    private static final Duration DUREE_VALIDITE = Duration.ofMinutes(10);
    private static final int MAX_TENTATIVES = 5;

    private final SecureRandom aleatoire = new SecureRandom();
    private final Map<String, Code> codesParEmail = new ConcurrentHashMap<>();
    private final Map<String, Integer> tentativesParEmail = new ConcurrentHashMap<>();

    /** Génère un code à 6 chiffres pour cet email et le renvoie (à envoyer par email). */
    public String generer(String email) {
        String code = String.format("%06d", aleatoire.nextInt(1_000_000));
        codesParEmail.put(email, new Code(code, Instant.now().plus(DUREE_VALIDITE)));
        tentativesParEmail.remove(email);
        return code;
    }

    /** Vrai si le code existe, n'est pas expiré, puis le consomme. */
    public boolean verifier(String code) {
        if (code == null) {
            return false;
        }
        return codesParEmail.entrySet().stream()
                .filter(entree -> entree.getValue().valeur().equals(code))
                .filter(entree -> entree.getValue().expiration().isAfter(Instant.now()))
                .findFirst()
                .map(entree -> {
                    codesParEmail.remove(entree.getKey());
                    return true;
                })
                .orElse(false);
    }

    /**
     * Vérifie le code d'UN email précis (à utiliser pour réinitialiser un mot de passe).
     * Le code est consommé s'il est bon, et invalidé après MAX_TENTATIVES mauvais essais.
     */
    public boolean verifier(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        Code enregistre = codesParEmail.get(email);
        if (enregistre == null) {
            return false;
        }
        if (!enregistre.expiration().isAfter(Instant.now())) {
            invalider(email);
            return false;
        }
        if (!egaux(enregistre.valeur(), code)) {
            int essais = tentativesParEmail.merge(email, 1, Integer::sum);
            if (essais >= MAX_TENTATIVES) {
                invalider(email);
            }
            return false;
        }
        invalider(email);
        return true;
    }

    private void invalider(String email) {
        codesParEmail.remove(email);
        tentativesParEmail.remove(email);
    }

    // Comparaison en temps constant : le temps de réponse ne révèle pas combien de chiffres sont justes.
    private static boolean egaux(String attendu, String saisi) {
        return MessageDigest.isEqual(
                attendu.getBytes(StandardCharsets.UTF_8), saisi.getBytes(StandardCharsets.UTF_8));
    }

    private record Code(String valeur, Instant expiration) {
    }
}