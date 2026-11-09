package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.BeneficiaireResponse;
import com.SecBank.pro.Dtos.CreationBeneficiaireRequest;
import com.SecBank.pro.Models.Cliens.Beneficiaire;
import com.SecBank.pro.Services.Clients.BeneficiaireService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client/beneficiaires")
public class BeneficiaireController {

    private final BeneficiaireService beneficiaireService;

    public BeneficiaireController(BeneficiaireService beneficiaireService) {
        this.beneficiaireService = beneficiaireService;
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaireResponse>> lister(
            Authentication authentication
    ) {
        List<BeneficiaireResponse> beneficiaires =
                beneficiaireService.listerBeneficiaires(authentication.getName())
                        .stream()
                        .map(BeneficiaireResponse::depuis)
                        .toList();

        return ResponseEntity.ok(beneficiaires);
    }

    @PostMapping
    public ResponseEntity<BeneficiaireResponse> creer(
            @RequestBody CreationBeneficiaireRequest requete,
            Authentication authentication
    ) {
        if (requete == null) {
            return ResponseEntity.badRequest().build();
        }

        Beneficiaire beneficiaire = beneficiaireService.creerBeneficiaire(
                authentication.getName(),
                requete.nom(),
                requete.prenom(),
                requete.numeroCompte()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BeneficiaireResponse.depuis(beneficiaire));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        beneficiaireService.supprimerBeneficiaire(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }
}