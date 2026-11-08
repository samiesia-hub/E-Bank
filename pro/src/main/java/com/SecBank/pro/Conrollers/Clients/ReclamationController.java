package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.CreerReclamationRequest;
import com.SecBank.pro.Dtos.ReclamationResponse;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Services.Clients.ReclamationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Réclamations du client connecté. Le client est identifié par l'email du token JWT,
 * jamais par un identifiant transmis par l'appelant.
 */
@RestController
@RequestMapping("/api/client/reclamations")
@Tag(name = "Reclamations", description = "Creation et suivi des reclamations client.")
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @Operation(summary = "Creer une reclamation",
            description = "Cree une reclamation pour le client indique. "
                    + "ATTENTION : l'appartenance du client n'est pas encore verifiee (voir docs/verification-api.md, ecart 3.2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reclamation creee"),
            @ApiResponse(responseCode = "404", description = "Client inexistant",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Client introuvable avec id :9999\"}"))),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReclamationResponse> creerReclamation(@RequestBody CreerReclamationRequest requete,
                                                                Authentication authentication) {
        Reclamation reclamation = reclamationService.creerReclamation(
                authentication.getName(), requete.objet(), requete.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReclamationResponse.depuis(reclamation));
    }

    @GetMapping
    public ResponseEntity<List<ReclamationResponse>> mesReclamations(Authentication authentication) {
        List<ReclamationResponse> reclamations = reclamationService
                .listerReclamationsDuClient(authentication.getName())
                .stream()
                .map(ReclamationResponse::depuis)
                .toList();
        return ResponseEntity.ok(reclamations);
    }

    @Operation(summary = "Suivre une reclamation",
            description = "Renvoie la reclamation avec son client, son ticket et ses reponses. "
                    + "ATTENTION : le proprietaire n'est pas encore verifie (IDOR documente, ecart 3.2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reclamation trouvee"),
            @ApiResponse(responseCode = "404", description = "Reclamation inexistante", content = @Content),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReclamationResponse> suivreReclamation(@PathVariable Long id,
                                                                 Authentication authentication) {
        Reclamation reclamation = reclamationService.consulterReclamationDuClient(id, authentication.getName());
        return ResponseEntity.ok(ReclamationResponse.depuis(reclamation));
    }
}