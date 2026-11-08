package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.PageResponse;
import com.SecBank.pro.Dtos.TransactionResponse;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Services.Admin.ExportTransactionsService;
import com.SecBank.pro.Services.Clients.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Historique des transactions, pagine et filtre.
 * Le client est identifie par l'email du token ; l'admin voit toute la banque.
 */
@RestController
@Tag(name = "Transactions", description = "Historique pagine et export CSV des transactions.")
public class TransactionController {

    private static final Sort PLUS_RECENTES_D_ABORD = Sort.by(Sort.Direction.DESC, "dateTransaction")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final int TAILLE_MAX = 100;

    private static final MediaType TYPE_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final TransactionService transactionService;
    private final ExportTransactionsService exportTransactionsService;

    public TransactionController(TransactionService transactionService,
                                 ExportTransactionsService exportTransactionsService) {
        this.transactionService = transactionService;
        this.exportTransactionsService = exportTransactionsService;
    }

    /** Historique d'un compte : le proprietaire uniquement, 403 sinon. */
    @Operation(summary = "Historique des transactions d'un compte",
            description = "Tri decroissant par date. Filtres optionnels : periode (`du`/`au`) et sens (CREDIT/DEBIT).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de transactions"),
            @ApiResponse(responseCode = "400", description = "Pagination invalide (size hors 1..100) ou periode inversee",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"size doit etre compris entre 1 et 100\"}"))),
            @ApiResponse(responseCode = "404", description = "Compte inexistant ou n'appartenant pas au client", content = @Content)
    })
    @GetMapping("/api/comptes/{id}/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> transactionsDuCompte(
            @Parameter(description = "Identifiant du compte", example = "2001") @PathVariable Long id,
            @Parameter(description = "Numero de page (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page (1 a 100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Date de debut incluse", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin incluse", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Sens du mouvement", example = "DEBIT")
            @RequestParam(required = false) SensTransaction sens,
            Authentication authentication) {

        verifierPeriode(du, au);
        Page<Transaction> transactions = transactionService.rechercherTransactionsDuCompte(
                id, authentication.getName(), du, au, sens, pageable(page, size));

        return ResponseEntity.ok(PageResponse.depuis(transactions.map(TransactionResponse::depuis)));
    }

    /** Historique de tous les comptes du client connecte, avec compte concerne optionnel. */
    @Operation(summary = "Historique des transactions du client connecte",
            description = "Agrege tous les comptes du client ; `compteId` restreint a un compte precis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de transactions"),
            @ApiResponse(responseCode = "400", description = "Pagination invalide ou periode inversee", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de CLIENT", content = @Content)
    })
    @GetMapping("/api/client/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> mesTransactions(
            @Parameter(description = "Restreindre a un compte du client", example = "2001")
            @RequestParam(required = false) Long compteId,
            @Parameter(description = "Numero de page (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page (1 a 100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) SensTransaction sens,
            Authentication authentication) {

        verifierPeriode(du, au);
        Page<Transaction> transactions = transactionService.rechercherTransactionsDuClient(
                authentication.getName(), compteId, du, au, sens, pageable(page, size));

        return ResponseEntity.ok(PageResponse.depuis(transactions.map(TransactionResponse::depuis)));
    }

    /** Supervision globale de la banque (reserve au role ADMIN par la configuration de securite). */
    @Operation(summary = "Supervision de toutes les transactions (administration)",
            description = "Meme pagination et memes filtres que l'export CSV.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de transactions de la banque"),
            @ApiResponse(responseCode = "400", description = "Pagination invalide ou periode inversee", content = @Content),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content)
    })
    @GetMapping("/api/admin/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> toutesLesTransactions(
            @Parameter(description = "Filtrer sur un client", example = "1005") @RequestParam(required = false) Long clientId,
            @Parameter(description = "Filtrer sur un compte", example = "2001") @RequestParam(required = false) Long compteId,
            @Parameter(description = "Numero de page (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page (1 a 100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) SensTransaction sens) {

        verifierPeriode(du, au);
        Page<Transaction> transactions = transactionService.rechercherToutesLesTransactions(
                clientId, compteId, du, au, sens, pageable(page, size));

        return ResponseEntity.ok(PageResponse.depuis(transactions.map(TransactionResponse::depuis)));
    }

    /**
     * Export CSV de la supervision : memes filtres que la liste paginee, sans pagination.
     * La periode est bornee et l'action est journalisee par ExportTransactionsService.
     * "export" est le nom officiel ; "export-csv" reste expose pour compatibilite.
     */
    @Operation(summary = "Exporter les transactions en CSV (administration)",
            description = "Flux CSV UTF-8 avec BOM, separateur point-virgule (Excel). "
                    + "Periode par defaut : 30 derniers jours ; 366 jours maximum ; periode inversee refusee. "
                    + "Chaque export est journalise (audit) au nom de l'administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier CSV (text/csv)",
                    content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "400", description = "Periode invalide ou superieure a 366 jours",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Periode maximale d'export depassee (366 jours)\"}"))),
            @ApiResponse(responseCode = "403", description = "Role different de ADMIN", content = @Content)
    })
    @GetMapping({"/api/admin/transactions/export", "/api/admin/transactions/export-csv"})
    public ResponseEntity<byte[]> exporterTransactionsCsv(
            @Parameter(description = "Filtrer sur un client", example = "1005") @RequestParam(required = false) Long clientId,
            @Parameter(description = "Filtrer sur un compte", example = "2001") @RequestParam(required = false) Long compteId,
            @Parameter(description = "Debut de periode", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Fin de periode", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Sens du mouvement", example = "CREDIT")
            @RequestParam(required = false) SensTransaction sens,
            Authentication authentication) {

        byte[] csv = exportTransactionsService.exporter(
                clientId, compteId, du, au, sens, authentication.getName());
        String nomFichier = "transactions-" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .contentType(TYPE_CSV)
                .body(csv);
    }

    private static Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page doit etre superieur ou egal a 0");
        }
        if (size < 1 || size > TAILLE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size doit etre compris entre 1 et " + TAILLE_MAX);
        }
        return PageRequest.of(page, size, PLUS_RECENTES_D_ABORD);
    }

    private static void verifierPeriode(LocalDate du, LocalDate au) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de debut doit preceder la date de fin");
        }
    }
}
