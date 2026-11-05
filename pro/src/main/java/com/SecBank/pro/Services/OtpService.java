package com.SecBank.pro.Services;

import org.springframework.stereotype.Service;

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

    private static final Duration DUREE_VALIDITE = Duration.ofMinutes(5);

    private final SecureRandom aleatoire = new SecureRandom();
    private final Map<String, Code> codesParEmail = new ConcurrentHashMap<>();

    /** Génère un code à 6 chiffres pour cet email et le renvoie (à envoyer par email). */
    public String generer(String email) {
        String code = String.format("%06d", aleatoire.nextInt(1_000_000));
        codesParEmail.put(email, new Code(code, Instant.now().plus(DUREE_VALIDITE)));
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

    private record Code(String valeur, Instant expiration) {
    }
}
