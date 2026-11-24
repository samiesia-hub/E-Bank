package com.SecBank.pro.Conrollers.Admin;

import com.SecBank.pro.Dtos.DashboardAdminResponse;
import com.SecBank.pro.Dtos.StatistiquesAdminResponse;
import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Models.Enums.StatutKYC;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import com.SecBank.pro.Repositories.Admin.TicketSupportRepo;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.DossierKYCRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tableau de bord de l'administrateur : indicateurs cles et activites recentes.
 *
 * Deux contrats attendus par le front (vue d'ensemble admin) :
 * - GET /api/admin/statistiques?periode=aujourdhui|7j|30j
 * - GET /api/admin/dashboard
 *
 * Routes reservees au role ADMIN par SecurityConfig (/api/admin/**).
 */
@RestController
@Tag(name = "Tableau de bord administrateur",
        description = "Indicateurs cles filtrables par periode et dernieres activites de la banque.")
public class AdminDashboardController {

    private static final List<StatusTicket> TICKETS_OUVERTS =
            List.of(StatusTicket.OUVERT, StatusTicket.EN_TRAITEMENT);

    private final ClientRepo clientRepo;
    private final UserRepo userRepo;
    private final TransactionRepository transactionRepository;
    private final TicketSupportRepo ticketSupportRepo;
    private final DossierKYCRepository dossierKYCRepository;
    private final JournalAuditRepository journalAuditRepository;

    public AdminDashboardController(ClientRepo clientRepo,
                                    UserRepo userRepo,
                                    TransactionRepository transactionRepository,
                                    TicketSupportRepo ticketSupportRepo,
                                    DossierKYCRepository dossierKYCRepository,
                                    JournalAuditRepository journalAuditRepository) {
        this.clientRepo = clientRepo;
        this.userRepo = userRepo;
        this.transactionRepository = transactionRepository;
        this.ticketSupportRepo = ticketSupportRepo;
        this.dossierKYCRepository = dossierKYCRepository;
        this.journalAuditRepository = journalAuditRepository;
    }

    @Operation(summary = "Indicateurs cles de la banque",
            description = "Total clients, total des depots, transactions de la periode et alertes fraude. "
                    + "Les tendances comparent la periode demandee a la periode precedente de meme duree.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicateurs renvoyes",
                    content = @Content(examples = @ExampleObject(value =
                            "{\"totalClients\":5,\"totalDepots\":{\"montant\":\"150000.00\",\"devise\":\"MAD\"},"
                                    + "\"transactionsDuJour\":27,"
                                    + "\"alertesFraude\":0,"
                                    + "\"tendances\":{\"totalClients\":0.0,\"totalDepots\":12.5,"
                                    + "\"transactionsDuJour\":-3.2,\"alertesFraude\":0.0}}"))),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content)
    })
    @GetMapping("/api/admin/statistiques")
    public ResponseEntity<StatistiquesAdminResponse> statistiques(
            @Parameter(description = "Periode observee", example = "7j",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"aujourdhui", "7j", "30j"}))
            @RequestParam(name = "periode", required = false, defaultValue = "7j") String periode) {

        int jours = nombreDeJours(periode);
        LocalDateTime debut = LocalDate.now().minusDays(jours - 1L).atStartOfDay();
        LocalDateTime fin = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime debutPrecedent = debut.minusDays(jours);

        long depotsPeriode = sommeDepots(debut, fin);
        long depotsPrecedent = sommeDepots(debutPrecedent, debut);
        long transactionsPeriode = transactionRepository.compterSurPeriode(debut, fin);
        long transactionsPrecedent = transactionRepository.compterSurPeriode(debutPrecedent, debut);
        long rejetsPeriode = transactionRepository.compterStatutSurPeriode(
                StatutTransaction.REJETEE, debut, fin);
        long rejetsPrecedent = transactionRepository.compterStatutSurPeriode(
                StatutTransaction.REJETEE, debutPrecedent, debut);

        long totalClients = clientRepo.count();

        StatistiquesAdminResponse.Tendances tendances = new StatistiquesAdminResponse.Tendances(
                // Aucune date de creation n'est stockee sur un utilisateur : la variation
                // du nombre total de clients ne peut pas encore etre calculee.
                0.0,
                StatistiquesAdminResponse.variation(
                        BigDecimal.valueOf(depotsPrecedent), BigDecimal.valueOf(depotsPeriode)),
                StatistiquesAdminResponse.variation(transactionsPrecedent, transactionsPeriode),
                StatistiquesAdminResponse.variation(rejetsPrecedent, rejetsPeriode)
        );

        return ResponseEntity.ok(new StatistiquesAdminResponse(
                totalClients,
                StatistiquesAdminResponse.Montant.de(BigDecimal.valueOf(depotsPeriode), "MAD"),
                transactionsPeriode,
                rejetsPeriode,
                tendances
        ));
    }

    @Operation(summary = "Tableau de bord de l'administrateur",
            description = "Utilisateurs actifs, tickets ouverts, dossiers KYC en attente et "
                    + "les 5 dernieres entrees du journal d'audit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tableau de bord renvoye",
                    content = @Content(examples = @ExampleObject(value =
                            "{\"utilisateursActifs\":9,\"ticketsOuverts\":3,"
                                    + "\"dossiersKycEnAttente\":5,"
                                    + "\"activitesRecentes\":[{\"id\":42,"
                                    + "\"action\":\"OUVERTURE_COMPTE\",\"auteur\":\"admin.demo1@demo.bankati.test\","
                                    + "\"date\":\"2026-11-14T09:12:00\"}]}"))),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content)
    })
    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<DashboardAdminResponse> tableauDeBord() {

        List<DashboardAdminResponse.ActiviteRecente> activites =
                journalAuditRepository.findTop5ByOrderByDateActionDesc()
                        .stream()
                        .map(AdminDashboardController::versActivite)
                        .toList();

        return ResponseEntity.ok(new DashboardAdminResponse(
                userRepo.countByActifTrue(),
                ticketSupportRepo.countByStatusIn(TICKETS_OUVERTS),
                dossierKYCRepository.countByStatutKYC(StatutKYC.EN_ATTENTE),
                activites
        ));
    }

    private long sommeDepots(LocalDateTime debut, LocalDateTime fin) {
        BigDecimal somme = transactionRepository.sommeMontant(
                SensTransaction.CREDIT, StatutTransaction.EXECUTEE, debut, fin);
        return somme == null ? 0L : somme.longValue();
    }

    private static DashboardAdminResponse.ActiviteRecente versActivite(JournalAudit entree) {
        Utilisateur auteur = entree.getUtilisateur();
        String nom = null;
        if (auteur != null) {
            nom = auteur.getEmail() != null ? auteur.getEmail() : auteur.getNom();
        }
        return new DashboardAdminResponse.ActiviteRecente(
                entree.getId(),
                entree.getAction(),
                nom == null ? "systeme" : nom,
                entree.getDateAction()
        );
    }

    /** "aujourdhui" = 1 jour, "7j" = 7 jours, "30j" = 30 jours ; toute autre valeur retombe sur 7 jours. */
    private static int nombreDeJours(String periode) {
        return switch (periode == null ? "7j" : periode) {
            case "aujourdhui" -> 1;
            case "30j" -> 30;
            default -> 7;
        };
    }
}
