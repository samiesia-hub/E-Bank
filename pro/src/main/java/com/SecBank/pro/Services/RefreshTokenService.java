package com.SecBank.pro.Services;


import com.SecBank.pro.Models.RefreshToken;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    // Générer un token aléatoire
    private String generateToken() {

        byte[] bytes = new byte[64];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // Calculer le hash SHA-256
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Créer un Refresh Token
    public String createRefreshToken(Utilisateur user) {

        String rawToken = generateToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashToken(rawToken))
                .user(user)
                .expiryDate(
                        Instant.now().plusMillis(refreshTokenExpiration)
                )
                .revoked(false)
                .build();

        repository.save(refreshToken);

        return rawToken;
    }

    // Vérifier le Refresh Token
    @Transactional
    public RefreshToken verifyRefreshToken(String rawToken) {

        RefreshToken token = repository
                .findByToken(hashToken(rawToken))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh Token invalide"
                ));

        if (token.isRevoked()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh Token révoqué"
            );
        }

        if (!token.getExpiryDate().isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh Token expiré"
            );
        }

        return token;
    }

    // Rotation du Refresh Token
    @Transactional
    public TokenRotation rotateRefreshToken(String rawToken) {

        RefreshToken oldToken = verifyRefreshToken(rawToken);

        Utilisateur user = oldToken.getUser();

        oldToken.setRevoked(true);

        repository.save(oldToken);

        String newRefreshToken = createRefreshToken(user);

        return new TokenRotation(user, newRefreshToken);
    }

    // Révoquer un Refresh Token
    @Transactional
    public void revokeRefreshToken(String rawToken) {

        RefreshToken token = verifyRefreshToken(rawToken);

        token.setRevoked(true);

        repository.save(token);
    }

    public record TokenRotation(
            Utilisateur user,
            String refreshToken
    ) {}
}