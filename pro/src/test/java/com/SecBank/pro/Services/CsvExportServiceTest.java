package com.SecBank.pro.Services;

import com.SecBank.pro.Dtos.TransactionResponse;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Services.Clients.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CsvExportService csvExportService;

    @Test
    void ecrire_produitEnteteEtUneLigneParTransaction() throws Exception {
        ByteArrayOutputStream flux = new ByteArrayOutputStream();

        csvExportService.ecrire(flux, List.of(transaction()));

        String csv = contenu(flux);
        String[] lignes = csv.split("\r\n");
        assertEquals(2, lignes.length);
        assertEquals("Date;Emetteur;Destinataire;Montant;Devise;Statut;Sens;Compte;Libelle", lignes[0]);
        assertEquals("05/01/2026 10:00:00;Jean Dupont;Proprietaire;120.00;MAD;EXECUTEE;DEBIT;0000000000010;Loyer",
                lignes[1]);
    }

    @Test
    void ecrire_commenceParLeBomUtf8() throws Exception {
        ByteArrayOutputStream flux = new ByteArrayOutputStream();

        csvExportService.ecrire(flux, List.of());

        byte[] octets = flux.toByteArray();
        assertEquals((byte) 0xEF, octets[0]);
        assertEquals((byte) 0xBB, octets[1]);
        assertEquals((byte) 0xBF, octets[2]);
    }

    @Test
    void ecrire_echappeLeSeparateurLesGuillemetsEtLesRetoursALigne() throws Exception {
        TransactionResponse transaction = new TransactionResponse(
                1L, LocalDateTime.of(2026, 1, 5, 10, 0), new BigDecimal("10"),
                "MAD", "CREDIT", "EXECUTEE", "Loyer; \"octobre\"\nligne 2",
                10L, "0000000000010", 1L, "Jean Dupont", "Proprietaire");

        String ligne = CsvExportService.ligne(transaction);

        assertTrue(ligne.contains("\"Loyer; \"\"octobre\"\"\nligne 2\""), ligne);
        assertTrue(ligne.endsWith(";\"Loyer; \"\"octobre\"\"\nligne 2\""), ligne);
    }

    @Test
    void ecrire_valeursNullesDonnentDesChampsVides() throws Exception {
        TransactionResponse transaction = new TransactionResponse(
                1L, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(";;;;;;;;", CsvExportService.ligne(transaction));
    }

    @Test
    void echapper_neTouchePasUneValeurSimple() {
        assertEquals("Guichet", CsvExportService.echapper("Guichet"));
        assertEquals("", CsvExportService.echapper(null));
        assertFalse(CsvExportService.echapper("Guichet").contains("\""));
    }

    @Test
    void exporter_appliqueLesFiltresEtEcritLesTransactions() throws Exception {
        Client client = new Client();
        client.setNom("Jean Dupont");

        CompteBancaire compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setDevise("MAD");
        compte.setClient(client);

        Virement virement = new Virement();
        virement.setId(5L);
        virement.setMontant(new BigDecimal("120.00"));
        virement.setSens(SensTransaction.DEBIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setDateTransaction(LocalDateTime.of(2026, 1, 5, 10, 0));
        virement.setBeneficiaire("Proprietaire");
        virement.setMotif("Loyer");
        virement.setCompte(compte);

        when(transactionService.toutesLesTransactionsPourExport(
                eq(1L), eq(10L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)),
                eq(SensTransaction.DEBIT)))
                .thenReturn(List.of(virement));

        ByteArrayOutputStream flux = new ByteArrayOutputStream();
        csvExportService.exporter(flux, 1L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                SensTransaction.DEBIT);

        String csv = contenu(flux);
        String[] lignes = csv.split("\r\n");
        assertEquals(2, lignes.length, csv);
        assertEquals("Date;Emetteur;Destinataire;Montant;Devise;Statut;Sens;Compte;Libelle", lignes[0]);
        assertEquals("05/01/2026 10:00:00;Jean Dupont;Proprietaire;120.00;MAD;EXECUTEE;DEBIT;0000000000010;Loyer",
                lignes[1]);

        verify(transactionService).toutesLesTransactionsPourExport(
                eq(1L), eq(10L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)),
                eq(SensTransaction.DEBIT));
    }

    private static TransactionResponse transaction() {
        return new TransactionResponse(
                5L,
                LocalDateTime.of(2026, 1, 5, 10, 0),
                new BigDecimal("120.00"),
                "MAD",
                "DEBIT",
                "EXECUTEE",
                "Loyer",
                10L,
                "0000000000010",
                1L,
                "Jean Dupont",
                "Proprietaire");
    }

    /** Decode le flux en retirant le BOM, qui n'est pas du texte metier. */
    private static String contenu(ByteArrayOutputStream flux) {
        String brut = flux.toString(StandardCharsets.UTF_8);
        return brut.startsWith("\uFEFF") ? brut.substring(1) : brut;
    }
}
