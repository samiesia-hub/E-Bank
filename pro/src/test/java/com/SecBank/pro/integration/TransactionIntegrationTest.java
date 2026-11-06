package com.SecBank.pro.integration;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du module transactions : historique pagine, calcul des
 * soldes et comportement lors d'operations successives (cas nominaux et erreurs).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    private static final String EMAIL_ADMIN = "admin@example.com";
    private static final String EMAIL_CLIENT = "sami@example.com";
    private static final String EMAIL_AUTRE_CLIENT = "autre@example.com";
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
    @DisplayName("Given 25 operations, When le client pagine par 10, Then les pages se suivent sans doublon")
    void historiquePagineSansDoublon() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "1000.00");

        for (int i = 1; i <= 25; i++) {
            deposer(tokenAdmin, compteId, "1.00", "op-" + i);
        }

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(10))
                .andExpect(jsonPath("$.dernierePage").value(false));

        List<Number> page0 = idsDeLaPage(tokenClient, compteId, 0, 10);
        List<Number> page1 = idsDeLaPage(tokenClient, compteId, 1, 10);
        List<Number> page2 = idsDeLaPage(tokenClient, compteId, 2, 10);

        assertEquals(10, page0.size());
        assertEquals(10, page1.size());
        assertEquals(5, page2.size());

        Set<Long> tousLesIds = new HashSet<>();
        for (List<Number> page : List.of(page0, page1, page2)) {
            page.forEach(id -> tousLesIds.add(id.longValue()));
        }
        assertEquals(25, tousLesIds.size(), "aucune transaction ne doit apparaitre deux fois");
    }

    @Test
    @DisplayName("Given plusieurs operations, When le client lit l'historique, Then les plus recentes arrivent en premier")
    void historiqueTrieDuPlusRecentAuPlusAncien() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "1000.00");

        deposer(tokenAdmin, compteId, "10.00", "op-1");
        deposer(tokenAdmin, compteId, "20.00", "op-2");
        deposer(tokenAdmin, compteId, "30.00", "op-3");

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu.length()").value(3))
                .andExpect(jsonPath("$.contenu[0].libelle").value("op-3"))
                .andExpect(jsonPath("$.contenu[1].libelle").value("op-2"))
                .andExpect(jsonPath("$.contenu[2].libelle").value("op-1"))
                .andExpect(jsonPath("$.contenu[0].sens").value("CREDIT"))
                .andExpect(jsonPath("$.contenu[0].statutTransaction").value("EXECUTEE"))
                .andExpect(jsonPath("$.contenu[0].numeroCompte").isNotEmpty())
                .andExpect(jsonPath("$.contenu[0].emetteur").isNotEmpty());
    }

    @Test
    @DisplayName("Given des operations de sens differents, When le client filtre par sens et periode, Then seules les bonnes lignes sortent")
    void historiqueFiltreParSensEtPeriode() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "1000.00");

        deposer(tokenAdmin, compteId, "150.00", "depot");
        retirer(tokenAdmin, compteId, "50.00", "retrait");

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("sens", "DEBIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].libelle").value("retrait"))
                .andExpect(jsonPath("$.contenu[0].sens").value("DEBIT"));

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("du", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("au", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Given des depots et retraits successifs, When le client consulte, Then le solde est exact")
    void calculDuSoldeApresOperationsSuccessives() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "0.00");

        MvcResult depot = deposer(tokenAdmin, compteId, "1000.00", "salaire");
        assertEquals(0, new BigDecimal("1000.00").compareTo(montant(contenu(depot), "$.nouveauSolde")));

        MvcResult retrait = retirer(tokenAdmin, compteId, "250.50", "loyer");
        assertEquals(0, new BigDecimal("749.50").compareTo(montant(contenu(retrait), "$.nouveauSolde")));

        deposer(tokenAdmin, compteId, "0.50", "remise");

        assertEquals(0, new BigDecimal("750.00").compareTo(solde(tokenClient, compteId)));
        assertEquals(3, transactionRepository.count());

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.contenu[0].libelle").value("remise"))
                .andExpect(jsonPath("$.contenu[1].sens").value("DEBIT"))
                .andExpect(jsonPath("$.contenu[2].sens").value("CREDIT"));
    }

    @Test
    @DisplayName("Given un solde insuffisant, When un retrait est demande, Then 400, solde intact et aucun mouvement")
    void retraitRefuseSiSoldeInsuffisant() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "40.00");

        mockMvc.perform(post("/api/admin/comptes/{id}/retrait", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":100.00,\"motif\":\"trop cher\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertEquals(0, new BigDecimal("40.00").compareTo(solde(tokenClient, compteId)));
        assertEquals(0, transactionRepository.count(), "le retrait refuse ne doit rien enregistrer");
    }

    @Test
    @DisplayName("Given un montant nul ou negatif, When une operation est demandee, Then 400 et solde intact")
    void operationRefuseeAvecUnMontantInvalide() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "0.00");

        for (String montant : List.of("0", "-50.00")) {
            mockMvc.perform(post("/api/admin/comptes/{id}/depot", compteId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"montant\":" + montant + ",\"motif\":\"invalide\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/admin/comptes/{id}/depot", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());

        assertEquals(0, new BigDecimal("0.00").compareTo(solde(tokenClient, compteId)));
        assertEquals(0, transactionRepository.count());
    }

    @Test
    @DisplayName("Given un compte gele, When une operation est demandee, Then 400 ; reactive, elle repasse")
    void operationRefuseeSurUnCompteGele() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "100.00");

        mockMvc.perform(patch("/api/admin/comptes/{id}/statut", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"BLOQUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutCompte").value("BLOQUE"));

        mockMvc.perform(post("/api/admin/comptes/{id}/depot", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":10.00,\"motif\":\"gele\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/comptes/{id}/retrait", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":10.00}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, new BigDecimal("100.00").compareTo(solde(tokenClient, compteId)));
        assertEquals(0, transactionRepository.count());

        mockMvc.perform(patch("/api/admin/comptes/{id}/statut", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACTIF\"}"))
                .andExpect(status().isOk());

        deposer(tokenAdmin, compteId, "10.00", "reactive");
        assertEquals(0, new BigDecimal("110.00").compareTo(solde(tokenClient, compteId)));
    }

    @Test
    @DisplayName("Given le compte d'un autre client, When un client lit l'historique, Then 403")
    void historiqueRefusePourUnClientQuiNePossedePasLeCompte() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "100.00");
        deposer(tokenAdmin, compteId, "10.00", "op-1");

        String tokenAutre = inscrire(EMAIL_AUTRE_CLIENT);

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAutre))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Given un compte inexistant, When le client lit l'historique, Then 404")
    void historiqueRetourne404PourUnCompteInexistant() throws Exception {
        String tokenClient = inscrire(EMAIL_CLIENT);

        mockMvc.perform(get("/api/comptes/{id}/transactions", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given une pagination hors bornes, When le client lit l'historique, Then 400")
    void paginationInvalideEstRefusee() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "0.00");

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given aucun token, When un appelant lit un historique, Then 401")
    void historiqueRefuseSansAuthentification() throws Exception {
        mockMvc.perform(get("/api/comptes/{id}/transactions", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given deux clients avec des operations, When l'un lit ses transactions, Then il ne voit que les siennes")
    void transactionsDuClientLimiteesAuClientConnecte() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteClient = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "500.00");
        deposer(tokenAdmin, compteClient, "10.00", "client-1");
        deposer(tokenAdmin, compteClient, "20.00", "client-2");

        inscrire(EMAIL_AUTRE_CLIENT);
        long compteAutre = creerCompte(tokenAdmin, idDuClient(EMAIL_AUTRE_CLIENT), "500.00");
        deposer(tokenAdmin, compteAutre, "99.00", "autre-client");

        mockMvc.perform(get("/api/client/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[0].libelle").value("client-2"))
                .andExpect(jsonPath("$.contenu[1].libelle").value("client-1"));
    }

    @Test
    @DisplayName("Given un depot au guichet, When il est enregistre, Then une entree d'audit est creee")
    void depotJournaliseDansLAudit() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "0.00");

        deposer(tokenAdmin, compteId, "75.00", "versement especes");

        List<JournalAudit> audits = journalAuditRepository.findAll();
        assertEquals(1, audits.size());
        assertEquals("DEPOT_GUICHET", audits.getFirst().getAction());
        assertEquals(EMAIL_ADMIN, audits.getFirst().getUtilisateur().getEmail());
        assertTrue(audits.getFirst().getDetails().contains("75.00"));
        assertFalse(audits.getFirst().getDetails().isBlank());
    }

    // --- Helpers ---------------------------------------------------------

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

    private String inscrire(String email) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"%s","nom":"Client de test"}
                                """.formatted(email, MOT_DE_PASSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return texte(contenu(resultat), "$.token");
    }

    private long idDuClient(String email) {
        return clientRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Client absent : " + email))
                .getId_Utilisateur();
    }

    private long creerCompte(String tokenAdmin, long clientId, String soldeInitial) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId
                                + ",\"typeCompte\":\"COURANT\",\"soldeInitial\":" + soldeInitial + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(contenu(resultat), "$.id")).longValue();
    }

    private MvcResult deposer(String tokenAdmin, long compteId, String montant, String motif)
            throws Exception {
        return mockMvc.perform(post("/api/admin/comptes/{id}/depot", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":" + montant + ",\"motif\":\"" + motif + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeOperation").value("DEPOT"))
                .andReturn();
    }

    private MvcResult retirer(String tokenAdmin, long compteId, String montant, String motif)
            throws Exception {
        return mockMvc.perform(post("/api/admin/comptes/{id}/retrait", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":" + montant + ",\"motif\":\"" + motif + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeOperation").value("RETRAIT"))
                .andReturn();
    }

    private List<Number> idsDeLaPage(String token, long compteId, int page, int taille) throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/comptes/{id}/transactions", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(taille)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(contenu(resultat), "$.contenu[*].id");
    }

    private BigDecimal solde(String token, long compteId) throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/comptes/{id}", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return montant(contenu(resultat), "$.solde");
    }

    private static String contenu(MvcResult resultat) throws Exception {
        return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String texte(String json, String chemin) {
        return JsonPath.read(json, chemin);
    }

    private static BigDecimal montant(String json, String chemin) {
        Object valeur = JsonPath.read(json, chemin);
        return new BigDecimal(valeur.toString());
    }
}
