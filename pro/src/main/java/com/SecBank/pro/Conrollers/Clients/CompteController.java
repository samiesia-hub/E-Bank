package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.CompteResponse;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Services.Clients.CompteService;
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
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    /** Comptes du client connecte, avec leur solde exact. */
    @GetMapping("/api/client/comptes")
    public ResponseEntity<List<CompteResponse>> mesComptes(Authentication authentication) {
        List<CompteResponse> comptes = compteService.consulterComptesDuClient(authentication.getName())
                .stream()
                .map(CompteResponse::depuis)
                .toList();

        return ResponseEntity.ok(comptes);
    }

    /** Details et solde d'un compte : appartenance verifiee pour un client. */
    @GetMapping("/api/comptes/{id}")
    public ResponseEntity<CompteResponse> detailCompte(@PathVariable Long id,
                                                       Authentication authentication) {
        CompteBancaire compte = estAdministrateur(authentication)
                ? compteService.consulterCompte(id)
                : compteService.consulterCompteDuClient(id, authentication.getName());

        return ResponseEntity.ok(CompteResponse.depuis(compte));
    }

    /** Creation d'un compte par un administrateur (numero de compte et RIB generes, ou RIB fourni). */
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
    @GetMapping("/api/admin/comptes")
    public ResponseEntity<List<CompteResponse>> tousLesComptes() {
        List<CompteResponse> comptes = compteService.listerTousLesComptes()
                .stream()
                .map(CompteResponse::depuis)
                .toList();

        return ResponseEntity.ok(comptes);
    }

    /** Gel (BLOQUE) ou reactivation (ACTIF) d'un compte client. */
    @PatchMapping("/api/admin/comptes/{id}/statut")
    public ResponseEntity<CompteResponse> changerStatut(@PathVariable Long id,
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

    public record CreationCompteRequete(Long clientId, TypeCompte typeCompte, String rib,
                                        BigDecimal soldeInitial) {
    }

    public record ChangementStatutRequete(StatutCompte statut) {
    }
}
