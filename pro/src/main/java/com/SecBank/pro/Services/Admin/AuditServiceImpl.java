package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** Les entrees les plus recentes d'abord. */
    private static final Sort PLUS_RECENTES_D_ABORD = Sort.by(Sort.Direction.DESC, "dateAction");

    private final JournalAuditRepository journalAuditRepository;

    @Override
    public List<JournalAudit> consulterJournauxAudit() {
        return journalAuditRepository.findAll(PLUS_RECENTES_D_ABORD);
    }

    @Override
    public JournalAudit journaliser(String action, Utilisateur utilisateur, String details) {
        JournalAudit entree = new JournalAudit();
        entree.setAction(action);
        entree.setDetails(details);
        entree.setUtilisateur(utilisateur);
        // Horodatage de l'action, genere cote serveur.
        entree.setDateAction(LocalDateTime.now());

        return journalAuditRepository.save(entree);
    }
}
