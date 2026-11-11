package com.SecBank.pro.Configurations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
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
import java.util.UUID;

/**
 * Crée et lit les tokens JWT de l'application.
 * Le secret vient de la variable d'environnement JWT_SECRET, jamais du code.
 *
 * Propriétés :
 *   jwt.secret              (obligatoire en production, 32 caractères minimum)
 *   jwt.expiration-ms       (défaut 900000 = 15 minutes)
 *   jwt.issuer              (défaut "secbank")
 *   jwt.allow-random-secret (défaut false ; true uniquement en développement)
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey cle;
    private final long dureeValiditeMs;
    private final String emetteur;
    private final JwtParser parser;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-ms:900000}") long dureeValiditeMs,
                      @Value("${jwt.issuer:secbank}") String emetteur,
                      @Value("${jwt.allow-random-secret:false}") boolean autoriserSecretAleatoire) {
        this.cle = construireCle(secret, autoriserSecretAleatoire);
        this.dureeValiditeMs = dureeValiditeMs;
        this.emetteur = emetteur;
        this.parser = Jwts.parser()
                .verifyWith(cle)
                .requireIssuer(emetteur)
                .build();
    }

    /**
     * Le token porte l'email (sujet), le rôle (claim "role"), un identifiant
     * unique (jti) et l'émetteur. Le frontend lit le rôle pour ses gardes de routes.
     */
    public String creerToken(String email, String role) {
        Date maintenant = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emetteur)
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

    public String extraireIdentifiant(String token) {
        return lireClaims(token).getId();
    }

    public boolean estValide(String token) {
        try {
            lireClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getDureeValiditeMs() {
        return dureeValiditeMs;
    }

    private Claims lireClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * HS256 exige une clé d'au moins 256 bits (32 octets). On utilise les octets
     * bruts du secret pour que le frontend (jose) puisse vérifier le même token
     * avec la même variable JWT_SECRET. Le secret doit donc faire 32 caractères
     * minimum, comme indiqué dans le README du frontend.
     *
     * Sans secret, l'application refuse de démarrer, sauf si
     * jwt.allow-random-secret=true (développement uniquement).
     */
    private static SecretKey construireCle(String secret, boolean autoriserSecretAleatoire) {
        if (secret == null || secret.isBlank()) {
            if (!autoriserSecretAleatoire) {
                throw new IllegalStateException(
                        "JWT_SECRET est absent. Définissez-le (32 caractères minimum) "
                                + "ou activez jwt.allow-random-secret=true en développement uniquement.");
            }
            log.warn("JWT_SECRET absent : une clé aléatoire est utilisée, les tokens "
                    + "seront invalidés à chaque redémarrage. Développement uniquement.");
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