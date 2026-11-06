package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.Clients.TransactionService;
import com.SecBank.pro.Services.CsvExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de la politique d'export : bornes de periode, journalisation
 * de l'action et branchement sur le generateur CSV (reel, non mocke).
 */
@ExtendWith(MockitoExtension.class)
class ExportTransactionsServiceTest {

    private static final String EMAIL_ADMIN = "admin@example.com";

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepo userRepo;

    private ExportTransactionsService exportTransactionsService;
    private Administrateur admin;

    @BeforeEach
    void setUp() {
        exportTransactionsService = new ExportTransactionsService(
                new CsvExportService(transactionService), auditService, userRepo);

        admin = new Administrateur();
        admin.setEmail(EMAIL_ADMIN);
        admin.setNom("Administrateur de test");
    }

    @Test
    @DisplayName("Given 50 transactions filtrees, When l'admin exporte, Then 50 lignes + entete et une entree d'audit")
    void exporter_produitUneLigneParTransactionEtAudite() {
        when(transactionService.toutesLesTransactionsPourExport(
                isNull(), isNull(), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), isNull()))
                .thenReturn(virements(50));
        when(userRepo.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));

        byte[] csv = exportTransactionsService.exporter(
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, EMAIL_ADMIN);

        String contenu = new String(csv, StandardCharsets.UTF_8);
        assertEquals(51, contenu.split("\r\n").length, "en-tete + 50 transactions");
        assertTrue(contenu.contains("op-50"), contenu);

        verify(auditService).journaliser(
                eq(ExportTransactionsService.ACTION_AUDIT),
                eq(admin),
                argThat(details -> details.contains("50 transaction")
                        && details.contains("2026-10-01")
                        && details.contains("2026-10-31")));
    }

    @Test
    @DisplayName("Given un export, Then le flux commence par le BOM UTF-8 attendu par Excel")
    void exporter_commenceParLeBomUtf8() {
        when(transactionService.toutesLesTransactionsPourExport(
                isNull(), isNull(), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(virements(1));
        when(userRepo.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));

        byte[] csv = exportTransactionsService.exporter(
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, EMAIL_ADMIN);

        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                Arrays.copyOf(csv, 3), "sans BOM, Excel casse les accents");
    }

    @Test
    @DisplayName("Given aucune borne, When l'admin exporte, Then la periode par defaut est de 30 jours")
    void exporter_appliqueLaPeriodeParDefaut() {
        when(transactionService.toutesLesTransactionsPourExport(
                isNull(), isNull(), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(List.of());
        when(userRepo.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));

        exportTransactionsService.exporter(null, null, null, null, null, EMAIL_ADMIN);

        ArgumentCaptor<LocalDate> du = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> au = ArgumentCaptor.forClass(LocalDate.class);
        verify(transactionService).toutesLesTransactionsPourExport(
                isNull(), isNull(), du.capture(), au.capture(), isNull());

        assertEquals(LocalDate.now(), au.getValue());
        assertEquals(LocalDate.now().minusDays(ExportTransactionsService.PERIODE_DEFAUT_JOURS - 1L),
                du.getValue());
    }

    @Test
    @DisplayName("Given une periode de plus de 366 jours, Then l'export est refuse sans lecture ni audit")
    void exporter_refuseUnePeriodeTropLongue() {
        DonneeInvalideException erreur = assertThrows(DonneeInvalideException.class,
                () -> exportTransactionsService.exporter(null, null,
                        LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 31), null, EMAIL_ADMIN));

        assertTrue(erreur.getMessage().contains("366"), erreur.getMessage());
        verifyNoInteractions(transactionService, auditService, userRepo);
    }

    @Test
    @DisplayName("Given une date de debut apres la date de fin, Then l'export est refuse")
    void exporter_refuseDesBornesInversees() {
        DonneeInvalideException erreur = assertThrows(DonneeInvalideException.class,
                () -> exportTransactionsService.exporter(null, null,
                        LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, EMAIL_ADMIN));

        assertTrue(erreur.getMessage().contains("debut"), erreur.getMessage());
        verifyNoInteractions(transactionService, auditService, userRepo);
    }

    @Test
    @DisplayName("Given un libelle avec point-virgule et guillemets, Then la colonne est echappee")
    void exporter_echappeLesChampsSpeciaux() {
        when(transactionService.toutesLesTransactionsPourExport(
                isNull(), isNull(), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(List.<Transaction>of(virement(1, "Libellé; avec \"guillemets\"")));
        when(userRepo.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));

        byte[] csv = exportTransactionsService.exporter(
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, EMAIL_ADMIN);

        String contenu = new String(csv, StandardCharsets.UTF_8);
        assertTrue(contenu.contains("\"Libellé; avec \"\"guillemets\"\"\""), contenu);
        assertEquals(2, contenu.split("\r\n").length);
    }

    // --- Helpers ---------------------------------------------------------

    private static List<Transaction> virements(int nombre) {
        List<Transaction> liste = new ArrayList<>();
        for (int i = 1; i <= nombre; i++) {
            liste.add(virement(i, "op-" + i));
        }
        return liste;
    }

    private static Virement virement(int index, String motif) {
        Client client = new Client();
        client.setNom("Client de test");

        CompteBancaire compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setDevise("MAD");
        compte.setClient(client);

        Virement virement = new Virement();
        virement.setId((long) index);
        virement.setDateTransaction(LocalDateTime.of(2026, 10, 10, 10, 0).plusMinutes(index));
        virement.setMontant(new BigDecimal("120.00"));
        virement.setSens(SensTransaction.DEBIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setBeneficiaire("Beneficiaire de test");
        virement.setMotif(motif);
        virement.setCompte(compte);
        return virement;
    }
}
