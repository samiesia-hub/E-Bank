package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.HistoriqueVirementResponse;
import com.SecBank.pro.Dtos.PageResponse;
import com.SecBank.pro.Dtos.RecuVirementResponse;
import com.SecBank.pro.Dtos.VirementRequest;
import com.SecBank.pro.Dtos.VirementResponse;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Services.Clients.VirementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
public class VirementController {

    private static final int TAILLE_MAX = 100;

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

    @GetMapping("/api/client/virements")
    public ResponseEntity<PageResponse<HistoriqueVirementResponse>> historique(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateDebut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFin,
            @RequestParam(required = false) StatutTransaction statut,
            Authentication authentication
    ) {
        verifierRoleClient(authentication);
        verifierPeriode(dateDebut, dateFin);

        Page<Virement> virements = virementService.consulterHistorique(
                authentication.getName(),
                dateDebut,
                dateFin,
                statut,
                pagination(page, size)
        );

        return ResponseEntity.ok(
                PageResponse.depuis(
                        virements.map(HistoriqueVirementResponse::depuis)
                )
        );
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

    private static Pageable pagination(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page doit être supérieur ou égal à 0"
            );
        }

        if (size < 1 || size > TAILLE_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size doit être compris entre 1 et " + TAILLE_MAX
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "dateTransaction")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    private static void verifierPeriode(
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        if (dateDebut != null
                && dateFin != null
                && dateDebut.isAfter(dateFin)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La date de début doit précéder la date de fin"
            );
        }
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