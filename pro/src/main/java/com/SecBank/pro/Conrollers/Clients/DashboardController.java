package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.DashboardClientResponse;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import com.SecBank.pro.Repositories.Client.VirementRepository;
import com.SecBank.pro.Services.Clients.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tableau de bord du client connecte.
 *
 * Le client est identifie par l'email du jeton JWT (jamais par un identifiant
 * transmis par l'appelant), comme dans les autres controleurs client.
 */
@RestController
@Tag(name = "Tableau de bord client",
        description = "Resume des comptes, des derniers virements et des reclamations en cours.")
public class DashboardController {

    private static final int NOMBRE_VIREMENTS = 5;

    private final CompteService compteService;
    private final VirementRepository virementRepository;
    private final ReclamationRepo reclamationRepo;

    public DashboardController(CompteService compteService,
                               VirementRepository virementRepository,
                               ReclamationRepo reclamationRepo) {
        this.compteService = compteService;
        this.virementRepository = virementRepository;
        this.reclamationRepo = reclamationRepo;
    }

    @Operation(summary = "Tableau de bord du client connecte",
            description = "Comptes avec solde, 5 derniers virements et nombre de reclamations ouvertes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resume renvoye"),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de CLIENT", content = @Content)
    })
    @GetMapping("/api/client/dashboard")
    public ResponseEntity<DashboardClientResponse> tableauDeBord(Authentication authentication) {
        String email = authentication.getName();

        List<DashboardClientResponse.CompteResume> comptes =
                compteService.consulterComptesDuClient(email).stream()
                        .map(DashboardController::resumeCompte)
                        .toList();

        List<DashboardClientResponse.VirementResume> virements =
                virementRepository
                        .findByCompteClientEmailOrderByDateTransactionDesc(
                                email,
                                PageRequest.of(0, NOMBRE_VIREMENTS))
                        .stream()
                        .map(DashboardController::resumeVirement)
                        .toList();

        long reclamationsOuvertes =
                reclamationRepo.findByClientEmailOrderByDateCreationDesc(email).stream()
                        .filter(DashboardController::estOuverte)
                        .count();

        return ResponseEntity.ok(
                new DashboardClientResponse(comptes, virements, reclamationsOuvertes));
    }

    private static DashboardClientResponse.CompteResume resumeCompte(CompteBancaire compte) {
        return new DashboardClientResponse.CompteResume(
                compte.getId(),
                compte.getNumeroCompte(),
                compte.getTypeCompte() == null ? null : compte.getTypeCompte().name(),
                compte.getSolde(),
                compte.getStatutCompte() == null ? null : compte.getStatutCompte().name()
        );
    }

    private static DashboardClientResponse.VirementResume resumeVirement(Virement virement) {
        return new DashboardClientResponse.VirementResume(
                virement.getId(),
                virement.getDateTransaction(),
                virement.getMontant(),
                virement.getBeneficiaire(),
                virement.getMotif(),
                virement.getStatutTransaction() == null
                        ? null
                        : virement.getStatutTransaction().name()
        );
    }

    /** Une reclamation est "ouverte" tant qu'elle n'est ni resolute ni fermee. */
    private static boolean estOuverte(Reclamation reclamation) {
        return reclamation.getStatus() == StatusReclamation.OUVERTE
                || reclamation.getStatus() == StatusReclamation.EN_COURS;
    }
}
