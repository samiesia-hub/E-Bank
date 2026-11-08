package com.SecBank.pro.Conrollers.Admin;

import com.SecBank.pro.Dtos.OperationRequest;
import com.SecBank.pro.Dtos.OperationResponse;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Services.Clients.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operations de guichet : depot et retrait d'especes par un administrateur.
 * L'agent auteur est l'utilisateur du token (email), jamais un id transmis par l'appelant.
 * Routes reservees au role ADMIN par SecurityConfig (/api/admin/**).
 */
@RestController
@Tag(name = "Operations de guichet", description = "Depot et retrait d'especes par un administrateur.")
public class OperationController {

    private final CompteService compteService;

    public OperationController(CompteService compteService) {
        this.compteService = compteService;
    }

    @Operation(summary = "Deposer des especes sur un compte",
            description = "Cree une transaction CREDIT EXECUTEE et une operation de guichet tracee au nom de l'agent du jeton.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Depot enregistre"),
            @ApiResponse(responseCode = "400", description = "Montant absent ou <= 0, compte inactif",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Le compte 2007 n'est pas actif (statut BLOQUE)\"}"))),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Compte inexistant", content = @Content)
    })
    @PostMapping("/api/admin/comptes/{id}/depot")
    public ResponseEntity<OperationResponse> depot(
            @Parameter(description = "Identifiant du compte", example = "2001") @PathVariable Long id,
                                                   @RequestBody(required = false) OperationRequest requete,
                                                   Authentication authentication) {
        OperationGuichet operation = compteService.deposer(
                id, montant(requete), motif(requete), emailAgent(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.depuis(operation));
    }

    @Operation(summary = "Retirer des especes d'un compte",
            description = "Refuse si le solde est insuffisant ou si le compte n'est pas ACTIF.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Retrait enregistre"),
            @ApiResponse(responseCode = "400", description = "Montant invalide, solde insuffisant ou compte inactif",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Solde insuffisant : solde 15000.00, retrait demande 999999\"}"))),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Compte inexistant", content = @Content)
    })
    @PostMapping("/api/admin/comptes/{id}/retrait")
    public ResponseEntity<OperationResponse> retrait(
            @Parameter(description = "Identifiant du compte", example = "2001") @PathVariable Long id,
                                                     @RequestBody(required = false) OperationRequest requete,
                                                     Authentication authentication) {
        OperationGuichet operation = compteService.retirer(
                id, montant(requete), motif(requete), emailAgent(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.depuis(operation));
    }

    private static java.math.BigDecimal montant(OperationRequest requete) {
        return requete == null ? null : requete.montant();
    }

    private static String motif(OperationRequest requete) {
        return requete == null ? null : requete.motif();
    }

    private static String emailAgent(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
