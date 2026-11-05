package com.SecBank.pro;

import com.SecBank.pro.Configurations.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("secret-de-test-32-caracteres-0001", 3_600_000);

    @Test
    void tokenCreeContientEmailEtEstValide() {
        String token = jwtService.creerToken("sami@example.com", "CLIENT");

        assertTrue(jwtService.estValide(token));
        assertEquals("sami@example.com", jwtService.extraireEmail(token));
    }

    @Test
    void tokenSigneAvecUnAutreSecretEstRefuse() {
        String token = new JwtService("autre-secret-32-caracteres-000002", 3_600_000)
                .creerToken("sami@example.com", "CLIENT");

        assertFalse(jwtService.estValide(token));
    }

    @Test
    void tokenExpireEstRefuse() {
        String token = new JwtService("secret-de-test-32-caracteres-0001", -1_000)
                .creerToken("sami@example.com", "CLIENT");

        assertFalse(jwtService.estValide(token));
    }

    @Test
    void texteQuelconqueEstRefuse() {
        assertFalse(jwtService.estValide("pas-un-token"));
    }
}
