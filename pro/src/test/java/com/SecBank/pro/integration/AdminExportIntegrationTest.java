package com.SecBank.pro.integration;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Utils.IbanGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration de l'export CSV admin : 50 lignes filtrees + entete,
 * echappement des champs, refus 403 pour un client, periode bornee et audit.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminExportIntegrationTest {

    private static final String EMAIL_ADMIN = "admin@example.com";
    private static final String EMAIL_CLIENT = "sami@example.com";
    private static final String MOT_DE_PASSE = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JournalAuditRepository journalAuditRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;

    @Autowired
    private ClientRepo clientRepo;

    @Autowired
    private UserRepo userRepo;

    @BeforeEach
    void viderLaBase() {
        journalAuditRepository.deleteAll();
        transactionRepository.deleteAll();
        compteBancaireRepository.deleteAll();
        clientRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Given un admin et 50 transactions filtrees, When il exporte, Then 50 lignes + entete et une entree d'audit")
    void exportRetourneUneLigneParTransactionFiltreePlusLEntete() throws Exception {
        CompteBancaire compte = creerCompte();
        for (int i = 1; i <= 50; i++) {
            insererVirement(compte, LocalDateTime.of(2026, 10, 1, 10, 0).plusMinutes(i), "op-" + i);
        }
        for (int i = 1; i <= 10; i++) {
            insererVirement(compte, LocalDateTime.of(2026, 9, 1, 10, 0).plusMinutes(i), "sept-" + i);
        }

        MvcResult resultat = mockMvc.perform(get("/api/admin/transactions/export")
                        .param("du", "2026-10-01")
                        .param("au", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".csv")))
                .andReturn();

        String csv = new String(resultat.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String[] lignes = csv.substring(1).split("\r\n");

        assertEquals(51, lignes.length, "entete + 50 transactions de la periode");
        assertTrue(lignes[0].startsWith("Date;Emetteur;Destinataire;Montant;Devise;Statut;Sens;Compte;Libelle"),
                lignes[0]);
        assertTrue(csv.contains("op-50"));
        assertFalse(csv.contains("sept-1"), "les transactions hors periode ne sont pas exportees");

        List<JournalAudit> audits = journalAuditRepository.findAll();
        assertEquals(1, audits.size(), "un export doit laisser une trace d'audit");
        assertEquals("EXPORT_CSV_TRANSACTIONS", audits.getFirst().getAction());
        assertEquals(EMAIL_ADMIN, audits.getFirst().getUtilisateur().getEmail());
        assertTrue(audits.getFirst().getDetails().contains("50"));
        assertTrue(audits.getFirst().getDetails().contains("2026-10-01"));
    }

    @Test
    @DisplayName("Given un champ avec point-virgule et guillemet, Then la colonne reste echappee dans le CSV")
    void exportEchappeLesChampsSpeciaux() throws Exception {
        CompteBancaire compte = creerCompte();
        insererVirement(compte, LocalDateTime.of(2026, 10, 15, 10, 0), "Libellé; avec \"guillemets\"");

        String csv = executerExport("/api/admin/transactions/export", "2026-10-01", "2026-10-31");
        String[] lignes = csv.substring(1).split("\r\n");

        assertEquals(2, lignes.length);
        assertTrue(lignes[1].contains("\"Libellé; avec \"\"guillemets\"\"\""), lignes[1]);
    }

    @Test
    @DisplayName("Given un client, When il demande l'export, Then 403 et aucune entree d'audit")
    void exportRefusePourUnClient() throws Exception {
        String tokenClient = jwtService.creerToken(EMAIL_CLIENT, "CLIENT");

        mockMvc.perform(get("/api/admin/transactions/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));

        assertEquals(0, journalAuditRepository.count());
    }

    @Test
    @DisplayName("Given aucun token, When un appelant demande l'export, Then 401")
    void exportRefuseSansAuthentification() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given une periode de plus de 366 jours, When l'admin exporte, Then 400 et rien n'est journalise")
    void exportRefuseUnePeriodeTropLongue() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/export")
                        .param("du", "2020-01-01")
                        .param("au", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());

        assertEquals(0, journalAuditRepository.count());
    }

    @Test
    @DisplayName("Given des bornes inversees, When l'admin exporte, Then 400")
    void exportRefuseDesBornesInversees() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/export")
                        .param("du", "2026-10-31")
                        .param("au", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given aucune borne, Then seuls les 30 derniers jours sont exportes")
    void exportAppliqueUnePeriodeParDefaut() throws Exception {
        CompteBancaire compte = creerCompte();
        insererVirement(compte, LocalDateTime.now().minusDays(60), "ancien");
        insererVirement(compte, LocalDateTime.now(), "recent");

        String csv = executerExport("/api/admin/transactions/export", null, null);

        assertEquals(2, csv.substring(1).split("\r\n").length,
                "entete + la seule transaction des 30 derniers jours");
        assertTrue(csv.contains("recent"));
        assertFalse(csv.contains("ancien"));
    }

    @Test
    @DisplayName("Given l'alias historique, When l'admin appelle export-csv, Then le meme fichier est renvoye")
    void aliasExportCsvResteFonctionnel() throws Exception {
        CompteBancaire compte = creerCompte();
        insererVirement(compte, LocalDateTime.of(2026, 10, 15, 10, 0), "op-alias");

        String csv = executerExport("/api/admin/transactions/export-csv", "2026-10-01", "2026-10-31");

        assertEquals(2, csv.substring(1).split("\r\n").length);
        assertTrue(csv.contains("op-alias"));
    }

    // --- Helpers ---------------------------------------------------------

    private String executerExport(String chemin, String du, String au) throws Exception {
        var requete = get(chemin).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin());
        if (du != null) {
            requete.param("du", du);
        }
        if (au != null) {
            requete.param("au", au);
        }

        MvcResult resultat = mockMvc.perform(requete)
                .andExpect(status().isOk())
                .andReturn();
        return new String(resultat.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String tokenAdmin() {
        if (userRepo.findByEmail(EMAIL_ADMIN).isEmpty()) {
            Administrateur admin = new Administrateur();
            admin.setEmail(EMAIL_ADMIN);
            admin.setUsername(EMAIL_ADMIN);
            admin.setNom("Administrateur de test");
            admin.setPassword(passwordEncoder.encode(MOT_DE_PASSE));
            admin.setRole(Role.ADMIN);
            admin.setActif(true);
            admin.setMatricule("ADM-0001");
            admin.setNiveuxAcce(1);
            userRepo.save(admin);
        }
        return jwtService.creerToken(EMAIL_ADMIN, "ADMIN");
    }

    private CompteBancaire creerCompte() {
        Client client = new Client();
        client.setEmail(EMAIL_CLIENT);
        client.setUsername(EMAIL_CLIENT);
        client.setNom("Client de test");
        client.setPassword(passwordEncoder.encode(MOT_DE_PASSE));
        client.setRole(Role.CLIENT);
        client.setActif(true);
        client.setNumeroClient("CLI-0001");
        Client enregistre = clientRepo.save(client);

        String numeroCompte = IbanGenerator.genererNumeroCompte();
        CompteBancaire compte = new CompteBancaire();
        compte.setNumeroCompte(numeroCompte);
        compte.setRib(IbanGenerator.genererRib(numeroCompte));
        compte.setDevise("MAD");
        compte.setSolde(new BigDecimal("5000.00"));
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setDateCreation(LocalDateTime.of(2026, 1, 1, 9, 0));
        compte.setClient(enregistre);

        return compteBancaireRepository.save(compte);
    }

    private void insererVirement(CompteBancaire compte, LocalDateTime date, String motif) {
        Virement virement = new Virement();
        virement.setCompte(compte);
        virement.setDateTransaction(date);
        virement.setMontant(new BigDecimal("120.00"));
        virement.setSens(SensTransaction.CREDIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setBeneficiaire("Beneficiaire de test");
        virement.setMotif(motif);
        transactionRepository.save(virement);
    }
}
