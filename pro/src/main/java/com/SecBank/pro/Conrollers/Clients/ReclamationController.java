package com.SecBank.pro.Conrollers.Clients;


import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Services.Clients.ReclamationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reclamations")
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @PostMapping
    public ResponseEntity<Reclamation> creerReclamation(
            @RequestParam Long clientId,
            @RequestParam String objet,
            @RequestParam String description) {

        Reclamation reclamation = reclamationService.creerReclamation(
                clientId, objet, description
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(reclamation);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reclamation> suivreReclamation(
            @PathVariable Long id) {

        Reclamation reclamation = reclamationService.suivreReclamation(id);

        return ResponseEntity.ok(reclamation);
    }
}