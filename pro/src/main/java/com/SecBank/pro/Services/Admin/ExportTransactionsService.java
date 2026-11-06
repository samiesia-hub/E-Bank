package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.CsvExportService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Politique de l'export CSV de supervision : borne la periode demandee,
 * delegue la generation du flux a {@link CsvExportService} puis journalise
 * l'action dans l'audit avec l'administrateur auteur.
 */
@Service
public class ExportTransactionsService {

    /** Action tracee dans le journal d'audit. */
    static final String ACTION_AUDIT = "EXPORT_CSV_TRANSACTIONS";
    /** Periode appliquee quand l'appelant ne fournit aucune borne. */
    static final int PERIODE_DEFAUT_JOURS = 30;
    /** Garde-fou : un export ne peut pas couvrir plus d'un an. */
    static final int PERIODE_MAX_JOURS = 366;

    private final CsvExportService csvExportService;
    private final AuditService auditService;
    private final UserRepo userRepo;

    public ExportTransactionsService(CsvExportService csvExportService,
                                     AuditService auditService,
                                     UserRepo userRepo) {
        this.csvExportService = csvExportService;
        this.auditService = auditService;
        this.userRepo = userRepo;
    }

    /**
     * Produit le CSV des transactions filtrees (meme filtres que la liste admin).
     *
     * @throws DonneeInvalideException si les bornes sont incoherentes ou depassent la periode maximale
     */
    public byte[] exporter(Long clientId, Long compteId, LocalDate du, LocalDate au,
                           SensTransaction sens, String emailAdmin) {

        Bornes bornes = bornes(du, au);

        ByteArrayOutputStream flux = new ByteArrayOutputStream();
        int lignes;
        try {
            lignes = csvExportService.exporter(flux, clientId, compteId, bornes.du(), bornes.au(), sens);
        } catch (IOException echec) {
            throw new UncheckedIOException("Echec de la generation du flux CSV", echec);
        }

        Utilisateur admin = userRepo.findByEmail(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Administrateur introuvable avec email :" + emailAdmin));

        auditService.journaliser(ACTION_AUDIT, admin, details(lignes, bornes, sens, clientId, compteId));

        return flux.toByteArray();
    }

    /**
     * Borne la periode : 30 derniers jours par defaut, 366 jours maximum,
     * date de debut obligatoirement anterieure ou egale a la date de fin.
     */
    Bornes bornes(LocalDate du, LocalDate au) {
        LocalDate fin = au == null ? LocalDate.now() : au;
        LocalDate debut = du == null ? fin.minusDays(PERIODE_DEFAUT_JOURS - 1L) : du;

        if (debut.isAfter(fin)) {
            throw new DonneeInvalideException("La date de debut doit preceder la date de fin");
        }
        if (ChronoUnit.DAYS.between(debut, fin) > PERIODE_MAX_JOURS) {
            throw new DonneeInvalideException(
                    "La periode d'export ne peut pas depasser " + PERIODE_MAX_JOURS + " jours");
        }

        return new Bornes(debut, fin);
    }

    private static String details(int lignes, Bornes bornes, SensTransaction sens,
                                  Long clientId, Long compteId) {
        StringBuilder details = new StringBuilder()
                .append("Export CSV de ").append(lignes).append(" transaction(s) du ")
                .append(bornes.du()).append(" au ").append(bornes.au());

        if (sens != null) {
            details.append(" (sens=").append(sens).append(')');
        }
        if (clientId != null) {
            details.append(" (client=").append(clientId).append(')');
        }
        if (compteId != null) {
            details.append(" (compte=").append(compteId).append(')');
        }
        return details.toString();
    }

    /** Periode effectivement utilisee pour l'export. */
    record Bornes(LocalDate du, LocalDate au) {
    }
}
