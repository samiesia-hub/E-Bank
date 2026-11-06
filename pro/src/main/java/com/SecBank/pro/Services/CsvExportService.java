package com.SecBank.pro.Services;

import com.SecBank.pro.Dtos.TransactionResponse;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Services.Clients.TransactionService;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generation du flux CSV des transactions pour la supervision admin.
 *
 * Format : UTF-8 avec BOM (Excel affiche correctement les accents), separateur
 * ";" (convention des tableurs francais), echappement RFC 4180 des valeurs
 * contenant le separateur, des guillemets ou des retours a la ligne.
 */
@Service
public class CsvExportService {

    public static final String SEPARATEUR = ";";
    public static final String FIN_DE_LIGNE = "\r\n";
    public static final List<String> ENTETES = List.of(
            "Date", "Emetteur", "Destinataire", "Montant", "Devise", "Statut", "Sens", "Compte", "Libelle");

    /** Marque d'ordre des octets UTF-8 : sans elle Excel lit les accents en Latin-1. */
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TransactionService transactionService;

    public CsvExportService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Ecrit l'entete puis une ligne par transaction correspondant aux filtres.
     * Le flux n'est pas ferme : il appartient a la reponse HTTP.
     */
    public void exporter(OutputStream flux, Long clientId, Long compteId,
                         LocalDate du, LocalDate au, SensTransaction sens) throws IOException {
        List<TransactionResponse> transactions = transactionService
                .toutesLesTransactionsPourExport(clientId, compteId, du, au, sens)
                .stream()
                .map(TransactionResponse::depuis)
                .toList();

        ecrire(flux, transactions);
    }

    public void ecrire(OutputStream flux, List<TransactionResponse> transactions) throws IOException {
        flux.write(BOM_UTF8);

        Writer writer = new BufferedWriter(new OutputStreamWriter(flux, StandardCharsets.UTF_8));
        writer.write(String.join(SEPARATEUR, ENTETES));
        writer.write(FIN_DE_LIGNE);

        for (TransactionResponse transaction : transactions) {
            writer.write(ligne(transaction));
            writer.write(FIN_DE_LIGNE);
        }

        // flush sans close : le flux de la reponse reste gere par le conteneur.
        writer.flush();
    }

    static String ligne(TransactionResponse transaction) {
        return String.join(SEPARATEUR,
                echapper(transaction.dateTransaction() == null
                        ? null
                        : FORMAT_DATE.format(transaction.dateTransaction())),
                echapper(transaction.emetteur()),
                echapper(transaction.destinataire()),
                montant(transaction.montant()),
                echapper(transaction.devise()),
                echapper(transaction.statutTransaction()),
                echapper(transaction.sens()),
                echapper(transaction.numeroCompte()),
                echapper(transaction.libelle()));
    }

    /** Montant non localise (point decimal) pour rester exploitable en machine. */
    private static String montant(BigDecimal valeur) {
        return valeur == null ? "" : valeur.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String echapper(String valeur) {
        if (valeur == null) {
            return "";
        }
        String propre = valeur.replace("\"", "\"\"");
        boolean aProteger = propre.contains(SEPARATEUR)
                || propre.contains("\"")
                || propre.contains("\n")
                || propre.contains("\r");

        return aProteger ? "\"" + propre + "\"" : propre;
    }
}
