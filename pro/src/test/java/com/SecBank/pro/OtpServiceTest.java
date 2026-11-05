package com.SecBank.pro;

import com.SecBank.pro.Services.OtpService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    private final OtpService otpService = new OtpService();

    @Test
    void codeGenereEstAccepteUneSeuleFois() {
        String code = otpService.generer("sami@example.com");

        assertTrue(otpService.verifier(code));
        assertFalse(otpService.verifier(code));
    }

    @Test
    void mauvaisCodeRefuse() {
        String code = otpService.generer("sami@example.com");
        String autreCode = code.equals("000000") ? "111111" : "000000";

        assertFalse(otpService.verifier(autreCode));
        assertFalse(otpService.verifier(null));
    }
}
