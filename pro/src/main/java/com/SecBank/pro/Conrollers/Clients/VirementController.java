package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.RecuVirementResponse;
import com.SecBank.pro.Dtos.VirementRequest;
import com.SecBank.pro.Dtos.VirementResponse;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Services.Clients.VirementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class VirementController {

    private final VirementService virementService;

    public VirementController(VirementService virementService) {
        this.virementService = virementService;
    }

    @PostMapping("/api/virements")
    public ResponseEntity<VirementResponse> effectuerVirement(
            @RequestBody VirementRequest requete,
            Authentication authentication
    ) {
        verifierRoleClient(authentication);

        if (requete == null
                || requete.compteSourceId() == null
                || requete.beneficiaireId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le compte source et le bénéficiaire sont obligatoires"
            );
        }

        Virement virement = virementService.effectuerVirement(
                authentication.getName(),
                requete.compteSourceId(),
                requete.beneficiaireId(),
                requete.montant(),
                requete.motif()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VirementResponse.depuis(virement));
    }

    @GetMapping("/api/client/virements/{reference}")
    public ResponseEntity<RecuVirementResponse> consulterRecu(
            @PathVariable String reference,
            Authentication authentication
    ) {
        verifierRoleClient(authentication);

        Virement virement = virementService.consulterRecu(
                authentication.getName(),
                reference
        );

        return ResponseEntity.ok(RecuVirementResponse.depuis(virement));
    }

    private static void verifierRoleClient(Authentication authentication) {
        boolean estClient = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(role -> "ROLE_CLIENT".equals(role.getAuthority()));

        if (!estClient) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette opération est réservée aux clients"
            );
        }
    }
}