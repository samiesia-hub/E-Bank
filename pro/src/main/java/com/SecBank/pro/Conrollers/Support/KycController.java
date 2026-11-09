package com.SecBank.pro.Conrollers.Support;

import com.SecBank.pro.Dtos.DossierKycResponse;
import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Services.Admin.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Validation KYC vue support (SCRUM-105 / SCRUM-139).
 * Accessible aux agents support et aux administrateurs ; le client recoit 403.
 */
@RestController
@Tag(name = "KYC support", description = "Consultation et decision sur les dossiers KYC en attente.")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    public record DecisionKycRequest(
            @Schema(description = "Motif du refus (obligatoire pour /reject)", example = "Piece d'identite illisible")
            String motif) {
    }

    @Operation(summary = "Dossiers KYC en attente",
            description = "Liste les dossiers au statut EN_ATTENTE, du plus ancien au plus recent. "
                    + "/api/support/kyc/pending reste un alias de cette route.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers en attente"),
            @ApiResponse(responseCode = "403", description = "Role AGENT_SUPPORT ou ADMIN requis", content = @Content)
    })
    @GetMapping({"/api/support/kyc", "/api/support/kyc/pending"})
    public ResponseEntity<List<DossierKycResponse>> dossiersEnAttente() {
        return ResponseEntity.ok(versReponses(kycService.listerDossiersEnAttente()));
    }

    @Operation(summary = "Valider un dossier KYC",
            description = "Passe le dossier a VALIDE et journalise la decision avec l'agent et l'horodatage.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier valide"),
            @ApiResponse(responseCode = "400", description = "Dossier deja traite",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"Le dossier KYC 4 a deja ete traite\"}"))),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant", content = @Content)
    })
    @PostMapping("/api/support/kyc/{id}/validate")
    public ResponseEntity<DossierKycResponse> valider(
            @Parameter(description = "Identifiant du dossier KYC", example = "4") @PathVariable Long id,
            Authentication authentication, HttpServletRequest requete) {
        DossierKYC dossier = kycService.validerDossierKYC(id, authentication.getName(), adresseIp(requete));
        return ResponseEntity.ok(DossierKycResponse.depuis(dossier));
    }

    @Operation(summary = "Refuser un dossier KYC",
            description = "Exige un motif. Le dossier passe a REJETE et la decision est journalisee ; "
                    + "sans motif l'API renvoie 400 et le dossier reste EN_ATTENTE.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier refuse"),
            @ApiResponse(responseCode = "400", description = "Motif absent ou dossier deja traite",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"Le motif de refus est obligatoire\"}"))),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant", content = @Content)
    })
    @PostMapping("/api/support/kyc/{id}/reject")
    public ResponseEntity<DossierKycResponse> rejeter(
            @Parameter(description = "Identifiant du dossier KYC", example = "4") @PathVariable Long id,
            @RequestBody DecisionKycRequest requete,
            Authentication authentication, HttpServletRequest httpRequete) {
        DossierKYC dossier = kycService.rejeterDossierKYC(
                id, requete.motif(), authentication.getName(), adresseIp(httpRequete));
        return ResponseEntity.ok(DossierKycResponse.depuis(dossier));
    }

    private String adresseIp(HttpServletRequest requete) {
        return requete.getRemoteAddr();
    }

    private List<DossierKycResponse> versReponses(List<DossierKYC> dossiers) {
        return dossiers.stream().map(DossierKycResponse::depuis).toList();
    }
}
