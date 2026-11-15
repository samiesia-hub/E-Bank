package com.SecBank.pro.Dtos;

/** Reponse de POST /api/auth/refresh : nouveau jeton d'acces et nouveau refresh token. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
