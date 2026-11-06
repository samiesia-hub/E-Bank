package com.SecBank.pro.integration;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Utils.IbanGenerator;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration du module comptes : ouverture d'un compte, consultation du
 * solde et restrictions de propriete. Toute la chaine HTTP -> securite JWT ->
 * service -> JPA est exercee sur la base H2 en memoire (src/test/resources).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CompteIntegrationTest {

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
        // Ordre impose par les cles etrangeres.
        journalAuditRepository.deleteAll();
        transactionRepository.deleteAll();
        compteBancaireRepository.deleteAll();
        clientRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Given un admin, When il ouvre un compte, Then le compte est ACTIF, en MAD, avec numero et RIB generes")
    void ouvertureDeCompteParAdminRetourneUnCompteActif() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long clientId = idDuClient(EMAIL_CLIENT);

        MvcResult resultat = mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"EPARGNE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCompte").isNotEmpty())
                .andExpect(jsonPath("$.devise").value("MAD"))
                .andExpect(jsonPath("$.typeCompte").value("EPARGNE"))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"))
                .andExpect(jsonPath("$.clientId").value((int) clientId))
                .andExpect(jsonPath("$.clientEmail").value(EMAIL_CLIENT))
                .andReturn();

        String corps = contenu(resultat);
        assertEquals(IbanGenerator.LONGUEUR_NUMERO_COMPTE,
                texte(corps, "$.numeroCompte").length(), "numero de compte sur 13 chiffres");
        assertEquals(IbanGenerator.LONGUEUR_RIB, texte(corps, "$.rib").length(), "RIB sur 24 chiffres");
        assertEquals(IbanGenerator.CODE_VILLE, texte(corps, "$.rib").substring(3, 6));
        assertEquals(0, new BigDecimal("0.00").compareTo(montant(corps, "$.solde")));

        // Le compte apparait dans la liste d'administration et pour son proprietaire.
        mockMvc.perform(get("/api/admin/comptes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/client/comptes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statutCompte").value("ACTIF"));
    }

    @Test
    @DisplayName("Given un solde initial, When le compte est ouvert, Then le solde est enregistre a l'identique")
    void ouvertureDeCompteConserveLeSoldeInitial() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        long clientId = idDuClient(EMAIL_CLIENT);

        MvcResult resultat = mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"COURANT\","
                                + "\"soldeInitial\":1250.75}"))
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(0, new BigDecimal("1250.75").compareTo(montant(contenu(resultat), "$.solde")));
    }

    @Test
    @DisplayName("Given un client, When il tente d'ouvrir un compte, Then 403 (droits admin requis)")
    void ouvertureDeCompteRefuseePourUnClient() throws Exception {
        String tokenClient = inscrire(EMAIL_CLIENT);
        long clientId = idDuClient(EMAIL_CLIENT);

        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"COURANT\"}"))
                .andExpect(status().isForbidden());

        assertEquals(0, compteBancaireRepository.count());
    }

    @Test
    @DisplayName("Given aucun token, When un appelant ouvre un compte, Then 401")
    void ouvertureDeCompteRefuseeSansAuthentification() throws Exception {
        mockMvc.perform(post("/api/admin/comptes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(0, compteBancaireRepository.count());
    }

    @Test
    @DisplayName("Given un clientId inconnu, When l'admin ouvre un compte, Then 404")
    void ouvertureDeCompteRefuseeSiClientInconnu() throws Exception {
        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":999999,\"typeCompte\":\"COURANT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given un solde initial negatif, When le compte est ouvert, Then 400")
    void ouvertureDeCompteRefuseeAvecSoldeInitialNegatif() throws Exception {
        inscrire(EMAIL_CLIENT);
        long clientId = idDuClient(EMAIL_CLIENT);

        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"COURANT\","
                                + "\"soldeInitial\":-10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertEquals(0, compteBancaireRepository.count());
    }

    @Test
    @DisplayName("Given un RIB au mauvais format, When le compte est ouvert, Then 400 avec le motif")
    void ouvertureDeCompteRefuseeAvecRibInvalide() throws Exception {
        inscrire(EMAIL_CLIENT);
        long clientId = idDuClient(EMAIL_CLIENT);

        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"COURANT\","
                                + "\"rib\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(IbanGenerator.MOTIF_RIB_FORMAT));
    }

    @Test
    @DisplayName("Given un RIB deja attribue, When l'admin ouvre un second compte, Then 409")
    void ouvertureDeCompteRefuseeAvecRibDejaUtilise() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        inscrire(EMAIL_AUTRE_CLIENT);
        String rib = IbanGenerator.genererRib();

        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + idDuClient(EMAIL_CLIENT)
                                + ",\"typeCompte\":\"COURANT\",\"rib\":\"" + rib + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + idDuClient(EMAIL_AUTRE_CLIENT)
                                + ",\"typeCompte\":\"COURANT\",\"rib\":\"" + rib + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertEquals(1, compteBancaireRepository.count());
    }

    @Test
    @DisplayName("Given le proprietaire du compte, When il consulte le solde, Then le solde exact est renvoye")
    void consultationDuSoldeParLeProprietaire() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "COURANT", "1250.75");

        mockMvc.perform(get("/api/comptes/{id}", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) compteId))
                .andExpect(jsonPath("$.clientEmail").value(EMAIL_CLIENT));

        assertEquals(0, new BigDecimal("1250.75").compareTo(solde(tokenClient, compteId)));
    }

    @Test
    @DisplayName("Given le compte d'un autre client, When un client le consulte, Then 404 sans reveler son existence")
    void consultationDuCompteDUnAutreClientRenvoie404() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "COURANT", "500.00");

        String tokenAutre = inscrire(EMAIL_AUTRE_CLIENT);

        mockMvc.perform(get("/api/comptes/{id}", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAutre))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given un admin, When il consulte le compte d'un client, Then 200")
    void consultationDuCompteParAdmin() throws Exception {
        String tokenAdmin = tokenAdmin();
        inscrire(EMAIL_CLIENT);
        long compteId = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "COURANT", "42.00");

        mockMvc.perform(get("/api/comptes/{id}", compteId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solde").value(42.00));
    }

    @Test
    @DisplayName("Given deux clients, When l'un liste ses comptes, Then il ne voit que les siens")
    void listeDesComptesLimiteeAuClientConnecte() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenClient = inscrire(EMAIL_CLIENT);
        long compteClient = creerCompte(tokenAdmin, idDuClient(EMAIL_CLIENT), "COURANT", "100.00");

        String tokenAutre = inscrire(EMAIL_AUTRE_CLIENT);
        creerCompte(tokenAdmin, idDuClient(EMAIL_AUTRE_CLIENT), "EPARGNE", "900.00");

        mockMvc.perform(get("/api/client/comptes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) compteClient));

        mockMvc.perform(get("/api/client/comptes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAutre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Given aucun token, When un appelant liste les comptes, Then 401")
    void listeDesComptesRefuseeSansAuthentification() throws Exception {
        mockMvc.perform(get("/api/client/comptes"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---------------------------------------------------------

    /** Cree (ou reutilise) l'administrateur de test et renvoie un token ADMIN valide. */
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

    private long creerCompte(String tokenAdmin, long clientId, String typeCompte, String soldeInitial)
            throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/admin/comptes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"typeCompte\":\"" + typeCompte
                                + "\",\"soldeInitial\":" + soldeInitial + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(contenu(resultat), "$.id")).longValue();
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
