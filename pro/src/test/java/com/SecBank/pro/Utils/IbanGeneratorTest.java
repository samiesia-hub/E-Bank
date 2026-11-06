package com.SecBank.pro.Utils;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbanGeneratorTest {

    private static final BigInteger MODULO = BigInteger.valueOf(97);

    @Test
    void genereUnRibMarocainValide() {
        String rib = IbanGenerator.genererRib("1234567890123");

        assertEquals(24, rib.length());
        assertTrue(rib.matches("\\d{24}"), rib);
        assertTrue(rib.startsWith("011780001"), rib);
        assertEquals("1234567890123", rib.substring(9, 22));
        assertEquals(0, new BigInteger(rib).mod(MODULO).intValue(),
                "la cle doit rendre le RIB divisible par 97");
        assertTrue(IbanGenerator.estValide(rib));
    }

    @Test
    void deuxGenerationsNeProduisentJamaisLeMemeRib() {
        Set<String> ribs = new HashSet<>();

        for (int i = 0; i < 2_000; i++) {
            String rib = IbanGenerator.genererRib();
            assertTrue(IbanGenerator.estValide(rib), rib);
            ribs.add(rib);
        }

        assertEquals(2_000, ribs.size(), "aucune collision de RIB attendue");
    }

    @Test
    void numeroDeCompteInvalide_refuseLaGeneration() {
        assertThrows(IllegalArgumentException.class, () -> IbanGenerator.genererRib("123"));
        assertThrows(IllegalArgumentException.class, () -> IbanGenerator.genererRib("12345678901234"));
        assertThrows(IllegalArgumentException.class, () -> IbanGenerator.genererRib(null));
    }

    @Test
    void valideUnRibSaisiEtEnExtraitLeNumeroDeCompte() {
        String rib = IbanGenerator.genererRib("9876543210123");

        assertTrue(IbanGenerator.estValide(rib));
        assertEquals("9876543210123", IbanGenerator.numeroCompteDepuisRib(rib));
    }

    @Test
    void refuseUnRibAbsentAvecLeMotifExact() {
        assertEquals(IbanGenerator.MOTIF_RIB_OBLIGATOIRE, IbanGenerator.raisonInvalidite(null));
        assertEquals(IbanGenerator.MOTIF_RIB_OBLIGATOIRE, IbanGenerator.raisonInvalidite("   "));
        assertFalse(IbanGenerator.estValide(null));
    }

    @Test
    void refuseUnRibDontLeFormatEstInvalide() {
        assertEquals(IbanGenerator.MOTIF_RIB_FORMAT, IbanGenerator.raisonInvalidite("123"));
        assertEquals(IbanGenerator.MOTIF_RIB_FORMAT, IbanGenerator.raisonInvalidite("0".repeat(25)));
        assertEquals(IbanGenerator.MOTIF_RIB_FORMAT, IbanGenerator.raisonInvalidite("0".repeat(23)));
        assertEquals(IbanGenerator.MOTIF_RIB_FORMAT, IbanGenerator.raisonInvalidite("a".repeat(24)));
    }

    @Test
    void refuseUnRibDontLaCleDeControleEstFausse() {
        String rib = IbanGenerator.genererRib("1234567890123");
        String cleFausse = String.format("%02d", (Integer.parseInt(rib.substring(22)) + 1) % 97);
        String ribFaux = rib.substring(0, 22) + cleFausse;

        assertEquals(IbanGenerator.MOTIF_RIB_CLE, IbanGenerator.raisonInvalidite(ribFaux));
        assertFalse(IbanGenerator.estValide(ribFaux));
    }
}
