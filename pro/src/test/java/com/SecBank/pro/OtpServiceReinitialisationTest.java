package com.SecBank.pro;

import com.SecBank.pro.Services.OtpService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceReinitialisationTest {

    private final OtpService otpService = new OtpService();

    @Test
    void codeCorrectPourLEmail_estAccepteUneSeuleFois() {
        String code = otpService.generer("jean@example.com");

        assertTrue(otpService.verifier("jean@example.com", code));
        assertFalse(otpService.verifier("jean@example.com", code));
    }

    @Test
    void codeNeValidePasPourUnAutreEmail() {
        String code = otpService.generer("jean@example.com");

        assertFalse(otpService.verifier("autre@example.com", code));
        assertTrue(otpService.verifier("jean@example.com", code));
    }

    @Test
    void cinqMauvaisEssaisInvalidentLeCode() {
        String code = otpService.generer("jean@example.com");
        String faux = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertFalse(otpService.verifier("jean@example.com", faux));
        }
        assertFalse(otpService.verifier("jean@example.com", code));
    }
}
