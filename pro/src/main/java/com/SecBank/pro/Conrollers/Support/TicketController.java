package com.SecBank.pro.Conrollers.Support;

import com.SecBank.pro.Dtos.TicketResponse;
import com.SecBank.pro.Models.Admin.TicketSupport;
import com.SecBank.pro.Models.Enums.Priorite;
import com.SecBank.pro.Models.Enums.StatusTicket;
import com.SecBank.pro.Services.Admin.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import java.util.List;

/**
 * Tickets support (SCRUM-107).
 * Le client ouvre et suit ses tickets ; le support consulte la file, assigne
 * un agent et fait evoluer le statut.
 */
@RestController
@Tag(name = "Tickets support", description = "Ouverture des tickets par le client et traitement par le support.")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public record CreationTicketRequest(
            @Schema(description = "Sujet du ticket", example = "Virement non recu") String objet,
            @Schema(description = "Description detaillee", example = "Le virement du 03/11 n'apparait pas.") String description,
            @Schema(description = "Priorite (facultative, MOYENNE par defaut)", example = "MOYENNE") Priorite priorite) {
    }

    public record ChangementStatutRequest(
            @Schema(description = "Nouveau statut", example = "EN_TRAITEMENT") StatusTicket statut) {
    }

    public record AffectationRequest(
            @Schema(description = "Identifiant de l'agent support", example = "12") Long agentId) {
    }

    @Operation(summary = "Ouvrir un ticket", description = "Cree un ticket OUVERT pour le client connecte (identifie par le jeton).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket cree"),
            @ApiResponse(responseCode = "400", description = "Objet ou description manquant",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"L'objet du ticket est obligatoire\"}"))),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide", content = @Content)
    })
    @PostMapping("/api/client/tickets")
    public ResponseEntity<TicketResponse> creerTicket(@RequestBody CreationTicketRequest requete,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.depuis(
                ticketService.creerTicket(authentication.getName(), requete.objet(), requete.description(), requete.priorite())));
    }

    @Operation(summary = "Mes tickets", description = "Tickets du client connecte, du plus recent au plus ancien.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Liste des tickets du client")
    @GetMapping("/api/client/tickets")
    public ResponseEntity<List<TicketResponse>> mesTickets(Authentication authentication) {
        return ResponseEntity.ok(versReponses(ticketService.consulterTicketsDuClient(authentication.getName())));
    }

    @Operation(summary = "File de tickets (support)", description = "Tous les tickets, du plus recent au plus ancien.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des tickets"),
            @ApiResponse(responseCode = "403", description = "Role AGENT_SUPPORT ou ADMIN requis", content = @Content)
    })
    @GetMapping("/api/support/tickets")
    public ResponseEntity<List<TicketResponse>> fileDeTickets() {
        return ResponseEntity.ok(versReponses(ticketService.consulterTickets()));
    }

    @Operation(summary = "Changer le statut d'un ticket",
            description = "Statuts possibles : OUVERT, EN_TRAITEMENT, EN_ATTENTE_CLIENT, FERME. "
                    + "La fermeture horodate la date de fermeture.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket mis a jour"),
            @ApiResponse(responseCode = "400", description = "Statut manquant", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ticket inexistant", content = @Content)
    })
    @PatchMapping("/api/support/tickets/{id}/status")
    public ResponseEntity<TicketResponse> changerStatut(
            @Parameter(description = "Identifiant du ticket", example = "5") @PathVariable Long id,
            @RequestBody ChangementStatutRequest requete) {
        return ResponseEntity.ok(TicketResponse.depuis(
                ticketService.mettreAJourStatutTicket(id, requete.statut())));
    }

    @Operation(summary = "Assigner un ticket a un agent",
            description = "Rattache le ticket a un agent support et le passe en EN_TRAITEMENT s'il etait OUVERT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket assigne"),
            @ApiResponse(responseCode = "404", description = "Ticket ou agent inexistant", content = @Content)
    })
    @PatchMapping("/api/support/tickets/{id}/assign")
    public ResponseEntity<TicketResponse> assigner(
            @Parameter(description = "Identifiant du ticket", example = "5") @PathVariable Long id,
            @RequestBody AffectationRequest requete) {
        return ResponseEntity.ok(TicketResponse.depuis(
                ticketService.assignerTicket(id, requete.agentId())));
    }

    private List<TicketResponse> versReponses(List<TicketSupport> tickets) {
        return tickets.stream().map(TicketResponse::depuis).toList();
    }
}
