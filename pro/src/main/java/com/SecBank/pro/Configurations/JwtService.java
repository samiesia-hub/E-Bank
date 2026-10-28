package com.SecBank.pro.Configurations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

/**
 * Crée et lit les tokens JWT de l'application.
 * Le secret vient de la variable d'environnement JWT_SECRET, jamais du code.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey cle;
    private final long dureeValiditeMs;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-ms:86400000}") long dureeValiditeMs) {
        this.cle = construireCle(secret);
        this.dureeValiditeMs = dureeValiditeMs;
    }

    /**
     * Le token porte l'email (sujet) et le rôle (claim "role"), car le frontend
     * lit ce rôle pour ses gardes de routes.
     */
    public String creerToken(String email, String role) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + dureeValiditeMs))
                .signWith(cle)
                .compact();
    }

    public String extraireEmail(String token) {
        return lireClaims(token).getSubject();
    }

    public String extraireRole(String token) {
        return lireClaims(token).get("role", String.class);
    }

    public boolean estValide(String token) {
        try {
            lireClaims(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public long getDureeValiditeMs() {
        return dureeValiditeMs;
    }

    private Claims lireClaims(String token) {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * HS256 exige une clé d'au moins 256 bits (32 octets). On utilise les octets
     * bruts du secret pour que le frontend (jose) puisse vérifier le même token
     * avec la même variable JWT_SECRET. Le secret doit donc faire 32 caractères
     * minimum, comme indiqué dans le README du frontend.
     */
    private static SecretKey construireCle(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET absent : une clé aléatoire est utilisée, les tokens "
                    + "seront invalidés à chaque redémarrage. Définir JWT_SECRET en production.");
            byte[] aleatoire = new byte[32];
            new SecureRandom().nextBytes(aleatoire);
            return Keys.hmacShaKeyFor(aleatoire);
        }

        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET doit contenir au moins 32 caractères (256 bits)");
        }
        return Keys.hmacShaKeyFor(octets);
    }
}
