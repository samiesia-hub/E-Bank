package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.CompteResponse;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Services.Clients.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Endpoints REST des comptes bancaires.
 * Le client est identifie par l'email du token JWT (jamais par un id transmis par l'appelant).
 */
@RestController
@Tag(name = "Comptes", description = "Consultation des comptes (client et administration).")
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    /** Comptes du client connecte, avec leur solde exact. */
    @Operation(summary = "Lister les comptes du client connecte",
            description = "Le client est identifie par le jeton : aucun identifiant client n'est accepte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comptes du client avec leur solde"),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de CLIENT", content = @Content)
    })
    @GetMapping("/api/client/comptes")
    public ResponseEntity<List<CompteResponse>> mesComptes(Authentication authentication) {
        List<CompteResponse> comptes = compteService.consulterComptesDuClient(authentication.getName())
                .stream()
                .map(CompteResponse::depuis)
                .toList();

        return ResponseEntity.ok(comptes);
    }

    /** Details et solde d'un compte : appartenance verifiee pour un client. */
    @Operation(summary = "Consulter un compte",
            description = "Un ADMIN voit tout compte ; un client ne voit que ses propres comptes (404 sinon, pour ne rien divulguer).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouve"),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content),
            @ApiResponse(responseCode = "404", description = "Compte inexistant ou n'appartenant pas au client",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Compte introuvable avec id :999999\"}")))
    })
    @GetMapping("/api/comptes/{id}")
    public ResponseEntity<CompteResponse> detailCompte(
            @Parameter(description = "Identifiant du compte", example = "2001") @PathVariable Long id,
                                                       Authentication authentication) {
        CompteBancaire compte = estAdministrateur(authentication)
                ? compteService.consulterCompte(id)
                : compteService.consulterCompteDuClient(id, authentication.getName());

        return ResponseEntity.ok(CompteResponse.depuis(compte));
    }

    /** Creation d'un compte par un administrateur (numero de compte et RIB generes, ou RIB fourni). */
    @Operation(summary = "Creer un compte client",
            description = "Numero de compte et RIB generes si `rib` est absent ; le solde initial alimente un depot d'ouverture.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte cree"),
            @ApiResponse(responseCode = "400", description = "clientId manquant, type de compte invalide ou RIB deja utilise",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"L'identifiant du client est obligatoire\"}"))),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client inexistant", content = @Content)
    })
    @PostMapping("/api/admin/comptes")
    public ResponseEntity<CompteResponse> creerCompte(@RequestBody CreationCompteRequete requete) {
        if (requete == null || requete.clientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant du client est obligatoire");
        }

        CompteBancaire compte = compteService.creerCompte(
                requete.clientId(), requete.typeCompte(), requete.rib(), requete.soldeInitial());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompteResponse.depuis(compte));
    }

    /** Liste de tous les comptes de la banque, titulaire inclus (administration). */
    @Operation(summary = "Lister tous les comptes (administration)",
            description = "Vue globale avec titulaire, type, statut et solde. Route reservee au role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des comptes"),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content)
    })
    @GetMapping("/api/admin/comptes")
    public ResponseEntity<List<CompteResponse>> tousLesComptes() {
        List<CompteResponse> comptes = compteService.listerTousLesComptes()
                .stream()
                .map(CompteResponse::depuis)
                .toList();

        return ResponseEntity.ok(comptes);
    }

    /** Gel (BLOQUE) ou reactivation (ACTIF) d'un compte client. */
    @Operation(summary = "Changer le statut d'un compte",
            description = "Bascule entre ACTIF et BLOQUE. La cloture n'est pas acceptee par cet endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis a jour"),
            @ApiResponse(responseCode = "400", description = "Statut manquant ou transition refusee (ex. CLOTURE)",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Le statut est obligatoire\"}"))),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Compte inexistant", content = @Content)
    })
    @PatchMapping("/api/admin/comptes/{id}/statut")
    public ResponseEntity<CompteResponse> changerStatut(
            @Parameter(description = "Identifiant du compte", example = "2001") @PathVariable Long id,
                                                        @RequestBody(required = false) ChangementStatutRequete requete) {
        if (requete == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut est obligatoire");
        }

        CompteBancaire compte = compteService.changerStatut(id, requete.statut());
        return ResponseEntity.ok(CompteResponse.depuis(compte));
    }

    private static boolean estAdministrateur(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authorite -> "ROLE_ADMIN".equals(authorite.getAuthority()));
    }

    @Schema(name = "CreationCompteRequete", description = "Creation d'un compte par un administrateur")
    public record CreationCompteRequete(
            @Schema(description = "Identifiant du client (utilisateur)", example = "1005") Long clientId,
            @Schema(description = "Type de compte", example = "COURANT") TypeCompte typeCompte,
            @Schema(description = "RIB fourni (facultatif, sinon genere)", example = "011780001100000000000149") String rib,
            @Schema(description = "Solde initial", example = "0.00") BigDecimal soldeInitial) {
    }

    @Schema(name = "ChangementStatutRequete", description = "Nouveau statut du compte")
    public record ChangementStatutRequete(
            @Schema(example = "BLOQUE") StatutCompte statut) {
    }
}
