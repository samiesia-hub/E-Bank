package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.CreerReclamationRequest;
import com.SecBank.pro.Dtos.ReclamationResponse;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Services.Clients.ReclamationService;
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
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ReclamationResponse> suivreReclamation(@PathVariable Long id,
                                                                 Authentication authentication) {
        Reclamation reclamation = reclamationService.consulterReclamationDuClient(id, authentication.getName());
        return ResponseEntity.ok(ReclamationResponse.depuis(reclamation));
    }
}