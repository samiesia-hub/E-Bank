package com.SecBank.pro.Utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * RIB marocain sur 24 chiffres : banque(3) + ville(3) + agence(3) + compte(13) + cle(2).
 * La cle est calculee pour que le RIB complet soit divisible par 97.
 *
 * Toutes les creations de compte passent par cet utilitaire, qu'il s'agisse
 * d'une generation aleatoire ou de la validation d'un RIB saisi par un agent.
 */
public final class IbanGenerator {

    public static final String PREFIXE_BANQUE = "011";
    public static final String CODE_VILLE = "780";
    public static final String CODE_AGENCE = "001";
    public static final int LONGUEUR_NUMERO_COMPTE = 13;
    public static final int LONGUEUR_RIB = 24;
    /** Banque + ville + agence : position de debut du numero de compte dans le RIB. */
    public static final int LONGUEUR_PREFIXE = PREFIXE_BANQUE.length() + CODE_VILLE.length() + CODE_AGENCE.length();

    // Motifs renvoyes tels quels au client (400) : messages stables et explicites.
    public static final String MOTIF_RIB_OBLIGATOIRE = "Le RIB est obligatoire";
    public static final String MOTIF_RIB_FORMAT = "Le RIB doit contenir exactement 24 chiffres";
    public static final String MOTIF_RIB_CLE = "La cle de controle du RIB est invalide";

    private static final BigInteger MODULO_RIB = BigInteger.valueOf(97);
    private static final BigInteger CENT = BigInteger.valueOf(100);
    private static final Pattern RIB_CHIFFRES = Pattern.compile("\\d{" + LONGUEUR_RIB + "}");
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private IbanGenerator() {
    }

    /** Numero de compte sur 13 chiffres, tire aleatoirement. */
    public static String genererNumeroCompte() {
        long valeur = ALEATOIRE.nextLong(1_000_000_000_000L);
        return String.format("%0" + LONGUEUR_NUMERO_COMPTE + "d", valeur);
    }

    /** RIB complet (numero de compte aleatoire + cle). */
    public static String genererRib() {
        return genererRib(genererNumeroCompte());
    }

    /**
     * RIB complet pour un numero de compte donne.
     *
     * @throws IllegalArgumentException si le numero de compte ne fait pas 13 chiffres
     */
    public static String genererRib(String numeroCompte) {
        if (numeroCompte == null
                || !numeroCompte.matches("\\d{" + LONGUEUR_NUMERO_COMPTE + "}")) {
            throw new IllegalArgumentException(
                    "Le numero de compte doit contenir " + LONGUEUR_NUMERO_COMPTE + " chiffres");
        }

        String base = PREFIXE_BANQUE + CODE_VILLE + CODE_AGENCE + numeroCompte;
        int reste = new BigInteger(base).multiply(CENT).mod(MODULO_RIB).intValue();
        int cle = (MODULO_RIB.intValue() - reste) % MODULO_RIB.intValue();

        String rib = base + String.format("%02d", cle);
        if (rib.length() != LONGUEUR_RIB) {
            throw new IllegalStateException("RIB genere invalide : " + rib);
        }
        return rib;
    }

    /** Numero de compte (13 chiffres) contenu dans un RIB valide. */
    public static String numeroCompteDepuisRib(String rib) {
        String motif = raisonInvalidite(rib);
        if (motif != null) {
            throw new IllegalArgumentException(motif);
        }
        return rib.trim().substring(LONGUEUR_PREFIXE, LONGUEUR_PREFIXE + LONGUEUR_NUMERO_COMPTE);
    }

    /** Format (24 chiffres) et cle de controle (modulo 97) valides. */
    public static boolean estValide(String rib) {
        return raisonInvalidite(rib) == null;
    }

    /**
     * Motif d'invalidite d'un RIB.
     *
     * @return {@code null} si le RIB est valide, sinon le message exact a renvoyer au client
     */
    public static String raisonInvalidite(String rib) {
        if (rib == null || rib.isBlank()) {
            return MOTIF_RIB_OBLIGATOIRE;
        }

        String normalise = rib.trim();
        if (!RIB_CHIFFRES.matcher(normalise).matches()) {
            return MOTIF_RIB_FORMAT;
        }
        if (new BigInteger(normalise).mod(MODULO_RIB).signum() != 0) {
            return MOTIF_RIB_CLE;
        }
        return null;
    }
}
