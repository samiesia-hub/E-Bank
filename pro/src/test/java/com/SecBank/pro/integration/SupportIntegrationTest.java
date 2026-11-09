package com.SecBank.pro.integration;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Admin.AgentSupport;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Enums.Role;
import com.SecBank.pro.Models.Enums.StatutKYC;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import com.SecBank.pro.Repositories.Admin.TicketSupportRepo;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.DossierKYCRepository;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import com.SecBank.pro.Repositories.UserRepo;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration Sprint 2 (support) : tickets et validation KYC de bout en
 * bout, chaine HTTP -> securite JWT -> service -> JPA sur base H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupportIntegrationTest {

    private static final String EMAIL_ADMIN = "admin@example.com";
    private static final String EMAIL_AGENT = "agent@example.com";
    private static final String EMAIL_CLIENT = "sami@example.com";
    private static final String MOT_DE_PASSE = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ClientRepo clientRepo;

    @Autowired
    private ReclamationRepo reclamationRepo;

    @Autowired
    private TicketSupportRepo ticketSupportRepo;

    @Autowired
    private DossierKYCRepository dossierKYCRepository;

    @Autowired
    private JournalAuditRepository journalAuditRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void viderLaBase() {
        // Les autres tests d'integration laissent des comptes, beneficiaires,
        // virements et transactions rattaches aux clients. On suspend donc les
        // contraintes le temps du nettoyage : supprimer un client encore
        // reference par un compte bancaire echouerait sinon.
        nettoyer();
    }

    @AfterEach
    void nettoyerApresLeTest() {
        // Toutes les classes de tests partagent la meme base H2 : ce nettoyage
        // evite que les reclamations creees ici bloquent les tests suivants.
        nettoyer();
    }

    private void nettoyer() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            journalAuditRepository.deleteAllInBatch();
            ticketSupportRepo.deleteAllInBatch();
            reclamationRepo.deleteAllInBatch();
            dossierKYCRepository.deleteAllInBatch();
            clientRepo.deleteAllInBatch();
            userRepo.deleteAllInBatch();
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    @Test
    @DisplayName("Given un client, When il ouvre un ticket, Then il est OUVERT et visible dans sa liste")
    void ouvertureDUnTicketParLeClient() throws Exception {
        String tokenClient = inscrire(EMAIL_CLIENT);

        mockMvc.perform(post("/api/client/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objet\":\"Virement absent\",\"description\":\"Rien recu depuis lundi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andExpect(jsonPath("$.priorite").value("MOYENNE"))
                .andExpect(jsonPath("$.objet").value("Virement absent"))
                .andExpect(jsonPath("$.clientEmail").value(EMAIL_CLIENT));

        mockMvc.perform(get("/api/client/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("OUVERT"));

        assertEquals(1, ticketSupportRepo.count());
    }

    @Test
    @DisplayName("Given un ticket, When l'agent support change son statut, Then il passe EN_TRAITEMENT puis FERME")
    void traitementDUnTicketParLeSupport() throws Exception {
        String tokenClient = inscrire(EMAIL_CLIENT);
        String tokenAgent = tokenAgent();
        long ticketId = creerTicket(tokenClient);

        mockMvc.perform(get("/api/support/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/api/support/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"EN_TRAITEMENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_TRAITEMENT"));

        mockMvc.perform(patch("/api/support/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"FERME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FERME"))
                .andExpect(jsonPath("$.dateFermeture").isNotEmpty());

        // Le client voit le nouveau statut sur son ticket.
        mockMvc.perform(get("/api/client/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("FERME"));
    }

    @Test
    @DisplayName("Given un client, When il appelle les routes support, Then 403")
    void clientRefuseSurLesRoutesSupport() throws Exception {
        String tokenClient = inscrire(EMAIL_CLIENT);
        long dossierId = creerDossierKyc(EMAIL_CLIENT);

        mockMvc.perform(get("/api/support/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/support/kyc").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/support/kyc/{id}/validate", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Given un dossier KYC en attente, When l'agent le valide, Then VALIDE et une trace d'audit est ecrite")
    void validationKycParAgent() throws Exception {
        inscrire(EMAIL_CLIENT);
        String tokenAgent = tokenAgent();
        long dossierId = creerDossierKyc(EMAIL_CLIENT);

        mockMvc.perform(get("/api/support/kyc").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$[0].clientEmail").value(EMAIL_CLIENT));

        mockMvc.perform(post("/api/support/kyc/{id}/validate", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));

        assertEquals(1, journalAuditRepository.count());
        mockMvc.perform(get("/api/support/kyc").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Given un dossier en attente, When le refus n'a pas de motif, Then 400 et le dossier reste EN_ATTENTE")
    void refusSansMotifRefuse() throws Exception {
        inscrire(EMAIL_CLIENT);
        String tokenAgent = tokenAgent();
        long dossierId = creerDossierKyc(EMAIL_CLIENT);

        mockMvc.perform(post("/api/support/kyc/{id}/reject", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le motif de refus est obligatoire"));

        assertEquals(StatutKYC.EN_ATTENTE,
                dossierKYCRepository.findById(dossierId).orElseThrow().getStatutKYC());
        assertEquals(0, journalAuditRepository.count());
    }

    @Test
    @DisplayName("Given un motif de refus, When l'agent rejette, Then REJETE et le motif est journalise")
    void refusAvecMotifJournalise() throws Exception {
        inscrire(EMAIL_CLIENT);
        String tokenAgent = tokenAgent();
        long dossierId = creerDossierKyc(EMAIL_CLIENT);

        mockMvc.perform(post("/api/support/kyc/{id}/reject", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Piece d'identite illisible\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"));

        String details = journalAuditRepository.findAll().getFirst().getDetails();
        assertTrue(details.contains("Piece d'identite illisible"), "le motif figure dans l'audit");
    }

    @Test
    @DisplayName("Given un administrateur, When il consulte la file KYC, Then il a aussi acces a l'espace support")
    void adminPeutAccederALespaceSupport() throws Exception {
        String tokenAdmin = tokenAdmin();

        mockMvc.perform(get("/api/support/kyc").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/support/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given un dossier deja valide, When une seconde validation arrive, Then 400")
    void dossierDejaTraiteRenvoie400() throws Exception {
        inscrire(EMAIL_CLIENT);
        String tokenAgent = tokenAgent();
        long dossierId = creerDossierKyc(EMAIL_CLIENT);

        mockMvc.perform(post("/api/support/kyc/{id}/validate", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/support/kyc/{id}/validate", dossierId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAgent))
                .andExpect(status().isBadRequest());

        assertEquals(1, journalAuditRepository.count());
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

    private String tokenAgent() {
        if (userRepo.findByEmail(EMAIL_AGENT).isEmpty()) {
            AgentSupport agent = new AgentSupport();
            agent.setEmail(EMAIL_AGENT);
            agent.setUsername(EMAIL_AGENT);
            agent.setNom("Agent de test");
            agent.setPassword(passwordEncoder.encode(MOT_DE_PASSE));
            agent.setRole(Role.SUPPORT);
            agent.setActif(true);
            agent.setMatricule("SUP-0001");
            userRepo.save(agent);
        }
        return jwtService.creerToken(EMAIL_AGENT, "AGENT_SUPPORT");
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
        return JsonPath.read(resultat.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");
    }

    private long creerTicket(String tokenClient) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/client/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objet\":\"Virement absent\",\"description\":\"Rien recu\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(
                resultat.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id")).longValue();
    }

    private long creerDossierKyc(String emailClient) {
        Client client = clientRepo.findByEmail(emailClient)
                .orElseThrow(() -> new IllegalStateException("Client absent : " + emailClient));

        DossierKYC dossier = new DossierKYC();
        dossier.setStatutKYC(StatutKYC.EN_ATTENTE);
        dossier.setDateSoumission(LocalDateTime.now().minusDays(1));
        dossier.setClient(client);
        return dossierKYCRepository.save(dossier).getId();
    }
}
