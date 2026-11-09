package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Admin.Administrateur;
import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Enums.StatutKYC;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Client.DossierKYCRepository;
import com.SecBank.pro.Repositories.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Validation et refus des dossiers KYC (SCRUM-105, SCRUM-139).
 *
 * Chaque decision est journalisee dans l'audit : c'est cette trace qui porte
 * l'agent decideur, l'horodatage et l'eventuel motif de refus.
 */
@Service
@AllArgsConstructor
public class KycServiceImpl implements KycService {

    private final DossierKYCRepository dossierKYCRepository;
    private final UserRepo userRepo;
    private final AuditService auditService;

    @Override
    public List<DossierKYC> listerDossiersEnAttente() {
        return dossierKYCRepository.findByStatutKYCOrderByDateSoumissionAsc(StatutKYC.EN_ATTENTE);
    }

    @Override
    @Transactional
    public DossierKYC validerDossierKYC(Long dossierId, String emailAgent, String adresseIp) {
        return decider(dossierId, StatutKYC.VALIDE, null, emailAgent, adresseIp);
    }

    @Override
    @Transactional
    public DossierKYC rejeterDossierKYC(Long dossierId, String motif, String emailAgent, String adresseIp) {
        if (motif == null || motif.isBlank()) {
            throw new DonneeInvalideException("Le motif de refus est obligatoire");
        }
        return decider(dossierId, StatutKYC.REJETE, motif.trim(), emailAgent, adresseIp);
    }

    private DossierKYC decider(Long dossierId, StatutKYC statut, String motif,
                               String emailAgent, String adresseIp) {
        DossierKYC dossier = dossierKYCRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier KYC introuvable avec id :" + dossierId));

        if (dossier.getStatutKYC() != StatutKYC.EN_ATTENTE) {
            throw new DonneeInvalideException("Le dossier KYC " + dossierId + " a deja ete traite");
        }

        Utilisateur decideur = userRepo.findByEmail(emailAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailAgent));

        dossier.setStatutKYC(statut);
        // Le modele ne stocke que l'administrateur : un agent support reste trace
        // par l'entree d'audit, qui reference son compte et la date de decision.
        if (decideur instanceof Administrateur administrateur) {
            dossier.setAdmin(administrateur);
        }
        DossierKYC dossierEnregistre = dossierKYCRepository.save(dossier);

        String action = statut == StatutKYC.VALIDE ? "KYC_VALIDATION" : "KYC_REFUS";
        String details = "Dossier KYC " + dossierId + " - client " + emailDuClient(dossier)
                + (motif == null ? "" : " - motif : " + motif);
        auditService.journaliser(action, decideur, details, adresseIp);

        return dossierEnregistre;
    }

    private String emailDuClient(DossierKYC dossier) {
        return dossier.getClient() == null ? "inconnu" : dossier.getClient().getEmail();
    }
}
