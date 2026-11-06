package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.PageResponse;
import com.SecBank.pro.Dtos.TransactionResponse;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Services.Admin.ExportTransactionsService;
import com.SecBank.pro.Services.Clients.TransactionService;
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
    @GetMapping("/api/comptes/{id}/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> transactionsDuCompte(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) SensTransaction sens,
            Authentication authentication) {

        verifierPeriode(du, au);
        Page<Transaction> transactions = transactionService.rechercherTransactionsDuCompte(
                id, authentication.getName(), du, au, sens, pageable(page, size));

        return ResponseEntity.ok(PageResponse.depuis(transactions.map(TransactionResponse::depuis)));
    }

    /** Historique de tous les comptes du client connecte, avec compte concerne optionnel. */
    @GetMapping("/api/client/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> mesTransactions(
            @RequestParam(required = false) Long compteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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
    @GetMapping("/api/admin/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> toutesLesTransactions(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long compteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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
    @GetMapping({"/api/admin/transactions/export", "/api/admin/transactions/export-csv"})
    public ResponseEntity<byte[]> exporterTransactionsCsv(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
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
