package com.SecBank.pro.Conrollers.Clients;


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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reclamations")
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
    public ResponseEntity<Reclamation> creerReclamation(
            @Parameter(description = "Identifiant du client", example = "1005") @RequestParam Long clientId,
            @Parameter(description = "Objet de la reclamation", example = "Frais bancaires inattendus")
            @RequestParam String objet,
            @Parameter(description = "Description detaillee", example = "Des frais ont ete preleves deux fois en octobre.")
            @RequestParam String description) {

        Reclamation reclamation = reclamationService.creerReclamation(
                clientId, objet, description
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(reclamation);
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
    public ResponseEntity<Reclamation> suivreReclamation(
            @Parameter(description = "Identifiant de la reclamation", example = "4001") @PathVariable Long id) {

        Reclamation reclamation = reclamationService.suivreReclamation(id);

        return ResponseEntity.ok(reclamation);
    }
}