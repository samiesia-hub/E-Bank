package com.SecBank.pro.Repositories.Client;

import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie sur une vraie base (H2) que les mises a jour de solde sont atomiques :
 * un mock ne prouverait rien sur le SQL genere.
 */
@DataJpaTest
class CompteBancaireRepositoryTest {

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;

    @Autowired
    private ClientRepo clientRepo;

    private CompteBancaire compte;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setEmail("jean@example.com");
        client.setUsername("jean");
        client.setActif(true);
        Client clientPersiste = clientRepo.save(client);

        compte = new CompteBancaire();
        compte.setNumeroCompte("0000000000010");
        compte.setRib("011780001000000000001045");
        compte.setDevise("MAD");
        compte.setSolde(new BigDecimal("1500.00"));
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setClient(clientPersiste);
        compte = compteBancaireRepository.save(compte);
    }

    @Test
    void incrementerSolde_augmenteLeSoldeEnBase() {
        int lignes = compteBancaireRepository.incrementerSolde(
                compte.getId(), new BigDecimal("500.00"), StatutCompte.ACTIF);

        assertEquals(1, lignes);
        assertSolde("2000.00");
    }

    @Test
    void decrementerSolde_retireLeMontantDemande() {
        int lignes = compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                compte.getId(), new BigDecimal("500.00"), StatutCompte.ACTIF);

        assertEquals(1, lignes);
        assertSolde("1000.00");
    }

    @Test
    void decrementerSolde_refuseSiSoldeInsuffisantEtNeModifieRien() {
        int lignes = compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                compte.getId(), new BigDecimal("5000.00"), StatutCompte.ACTIF);

        assertEquals(0, lignes);
        assertSolde("1500.00");
    }

    @Test
    void incrementerSolde_refuseSurCompteNonActif() {
        compte.setStatutCompte(StatutCompte.BLOQUE);
        compteBancaireRepository.save(compte);

        int lignes = compteBancaireRepository.incrementerSolde(
                compte.getId(), new BigDecimal("500.00"), StatutCompte.ACTIF);

        assertEquals(0, lignes);
        assertSolde("1500.00");
    }

    private void assertSolde(String attendu) {
        BigDecimal solde = compteBancaireRepository.findById(compte.getId())
                .orElseThrow()
                .getSolde();

        assertEquals(0, new BigDecimal(attendu).compareTo(solde),
                "solde attendu " + attendu + " mais obtenu " + solde);
    }
}
