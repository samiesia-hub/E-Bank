package com.SecBank.pro.Conrollers.Admin;

import com.SecBank.pro.Dtos.OperationRequest;
import com.SecBank.pro.Dtos.OperationResponse;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Services.Clients.CompteService;
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
public class OperationController {

    private final CompteService compteService;

    public OperationController(CompteService compteService) {
        this.compteService = compteService;
    }

    @PostMapping("/api/admin/comptes/{id}/depot")
    public ResponseEntity<OperationResponse> depot(@PathVariable Long id,
                                                   @RequestBody(required = false) OperationRequest requete,
                                                   Authentication authentication) {
        OperationGuichet operation = compteService.deposer(
                id, montant(requete), motif(requete), emailAgent(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse.depuis(operation));
    }

    @PostMapping("/api/admin/comptes/{id}/retrait")
    public ResponseEntity<OperationResponse> retrait(@PathVariable Long id,
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
