package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.InsufficientFundsException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Beneficiaire;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Repositories.Client.BeneficiaireRepository;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.VirementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class VirementServiceImpl implements VirementService {

    private static final Logger log = LoggerFactory.getLogger(VirementServiceImpl.class);

    private final CompteBancaireRepository compteRepository;
    private final BeneficiaireRepository beneficiaireRepository;
    private final VirementRepository virementRepository;

    public VirementServiceImpl(
            CompteBancaireRepository compteRepository,
            BeneficiaireRepository beneficiaireRepository,
            VirementRepository virementRepository
    ) {
        this.compteRepository = compteRepository;
        this.beneficiaireRepository = beneficiaireRepository;
        this.virementRepository = virementRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Virement effectuerVirement(
            String emailClient,
            Long compteSourceId,
            Long beneficiaireId,
            BigDecimal montant,
            String motif
    ) {
        log.info(
                "Demande de virement reçue : compteSourceId={}, beneficiaireId={}, montant={}",
                compteSourceId,
                beneficiaireId,
                montant
        );

        verifierMontant(montant);

        CompteBancaire compteSource = compteRepository.findById(compteSourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte source introuvable"
                ));

        verifierProprietaire(compteSource, emailClient);
        verifierCompteActif(compteSource, "source");

        Beneficiaire beneficiaire = beneficiaireRepository.findById(beneficiaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bénéficiaire introuvable"
                ));

        verifierBeneficiaire(beneficiaire, emailClient);

        CompteBancaire compteDestinataire = compteRepository
                .findByNumeroCompteOrRib(
                        beneficiaire.getNumeroCompte(),
                        beneficiaire.getNumeroCompte()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte destinataire introuvable"
                ));

        String nomBeneficiaire = (
                beneficiaire.getNom() + " " + beneficiaire.getPrenom()
        ).trim();

        return executerVirement(
                compteSource,
                compteDestinataire,
                nomBeneficiaire,
                montant,
                motif
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Virement effectuerVirementVersCompte(
            String emailClient,
            Long compteSourceId,
            String numeroCompteBeneficiaire,
            BigDecimal montant,
            String motif
    ) {
        log.info(
                "Demande de virement vers compte reçue : compteSourceId={}, montant={}",
                compteSourceId,
                montant
        );

        verifierMontant(montant);

        String identifiant = normaliserIdentifiant(numeroCompteBeneficiaire);

        if (identifiant.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le RIB ou le numero de compte du beneficiaire est obligatoire"
            );
        }

        CompteBancaire compteSource = compteRepository.findById(compteSourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte source introuvable"
                ));

        verifierProprietaire(compteSource, emailClient);
        verifierCompteActif(compteSource, "source");

        CompteBancaire compteDestinataire = compteRepository
                .findByNumeroCompteOrRib(identifiant, identifiant)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun compte Orabank ne correspond a ce RIB ou numero de compte"
                ));

        String nomBeneficiaire = compteDestinataire.getClient() == null
                || compteDestinataire.getClient().getNom() == null
                || compteDestinataire.getClient().getNom().isBlank()
                ? "Beneficiaire"
                : compteDestinataire.getClient().getNom().trim();

        return executerVirement(
                compteSource,
                compteDestinataire,
                nomBeneficiaire,
                montant,
                motif
        );
    }

    private Virement executerVirement(
            CompteBancaire compteSource,
            CompteBancaire compteDestinataire,
            String nomBeneficiaire,
            BigDecimal montant,
            String motif
    ) {
        verifierCompteActif(compteDestinataire, "destinataire");

        if (compteSource.getId().equals(compteDestinataire.getId())) {
            log.warn(
                    "Virement refusé : le compte source et le compte destinataire sont identiques"
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le compte source et le compte destinataire doivent être différents"
            );
        }

        verifierDevise(compteSource, compteDestinataire);

        int debitEffectue = compteRepository.decrementerSoldeSiSoldeSuffisant(
                compteSource.getId(),
                montant,
                StatutCompte.ACTIF
        );

        if (debitEffectue != 1) {
            log.warn(
                    "Virement refusé : solde insuffisant, compteSourceId={}, montant={}",
                    compteSource.getId(),
                    montant
            );

            throw new InsufficientFundsException(
                    "Solde insuffisant pour effectuer ce virement"
            );
        }

        int creditEffectue = compteRepository.incrementerSolde(
                compteDestinataire.getId(),
                montant,
                StatutCompte.ACTIF
        );

        if (creditEffectue != 1) {
            log.error(
                    "Virement annulé : impossible de créditer le compte destinataire"
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le compte destinataire n'est plus actif"
            );
        }

        CompteBancaire sourcePourTrace = compteRepository
                .findById(compteSource.getId())
                .orElseThrow();

        CompteBancaire destinationPourTrace = compteRepository
                .findById(compteDestinataire.getId())
                .orElseThrow();

        LocalDateTime maintenant = LocalDateTime.now();
        String motifValide = nettoyerMotif(motif);

        Virement ligneDebit = new Virement();
        ligneDebit.setCompte(sourcePourTrace);
        ligneDebit.setCompteDestinataire(destinationPourTrace);
        ligneDebit.setMontant(montant);
        ligneDebit.setSens(SensTransaction.DEBIT);
        ligneDebit.setStatutTransaction(StatutTransaction.EXECUTEE);
        ligneDebit.setDateTransaction(maintenant);
        ligneDebit.setBeneficiaire(nomBeneficiaire);
        ligneDebit.setMotif(motifValide);

        Virement debitSauvegarde = virementRepository.save(ligneDebit);

        debitSauvegarde.setReference(
                genererReference(maintenant, debitSauvegarde.getId())
        );

        Virement ligneCredit = new Virement();
        ligneCredit.setCompte(destinationPourTrace);
        ligneCredit.setMontant(montant);
        ligneCredit.setSens(SensTransaction.CREDIT);
        ligneCredit.setStatutTransaction(StatutTransaction.EXECUTEE);
        ligneCredit.setDateTransaction(maintenant);
        ligneCredit.setBeneficiaire(
                sourcePourTrace.getClient().getNom() == null
                        ? "Virement reçu"
                        : sourcePourTrace.getClient().getNom()
        );
        ligneCredit.setMotif(motifValide);

        virementRepository.save(ligneCredit);

        Virement resultat = virementRepository.save(debitSauvegarde);

        log.info(
                "Virement exécuté avec succès : reference={}, montant={}",
                resultat.getReference(),
                montant
        );

        return resultat;
    }

    @Override
    @Transactional(readOnly = true)
    public Virement consulterRecu(
            String emailClient,
            String reference
    ) {
        String referenceNormalisee = reference.trim().toUpperCase(Locale.ROOT);

        Virement virement = virementRepository
                .findByReference(referenceNormalisee)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Virement introuvable"
                ));

        verifierProprietaire(virement.getCompte(), emailClient);

        if (virement.getCompteDestinataire() == null) {
            throw new ResourceNotFoundException("Reçu de virement introuvable");
        }

        log.info("Reçu de virement consulté : reference={}", referenceNormalisee);

        return virement;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Virement> consulterHistorique(
            String emailClient,
            LocalDate dateDebut,
            LocalDate dateFin,
            StatutTransaction statut,
            Pageable pageable
    ) {
        LocalDateTime debut = dateDebut != null
                ? dateDebut.atStartOfDay()
                : LocalDate.of(1970, 1, 1).atStartOfDay();

        LocalDateTime finExclusive = dateFin != null
                ? dateFin.plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        return virementRepository.rechercherHistoriqueDuClient(
                emailClient,
                SensTransaction.DEBIT,
                debut,
                finExclusive,
                statut,
                pageable
        );
    }

    private static String genererReference(
            LocalDateTime date,
            Long identifiant
    ) {
        String jour = date.format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        return String.format(
                Locale.ROOT,
                "VIR-%s-%06d",
                jour,
                identifiant
        );
    }

    private static void verifierMontant(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le montant doit être strictement positif"
            );
        }
    }

    private static void verifierProprietaire(
            CompteBancaire compte,
            String emailClient
    ) {
        if (compte.getClient() == null
                || !emailClient.equalsIgnoreCase(
                compte.getClient().getEmail()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce virement ne vous appartient pas"
            );
        }
    }

    private static void verifierBeneficiaire(
            Beneficiaire beneficiaire,
            String emailClient
    ) {
        if (beneficiaire.getClient() == null
                || !emailClient.equalsIgnoreCase(
                beneficiaire.getClient().getEmail()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce bénéficiaire ne vous appartient pas"
            );
        }
    }

    private static void verifierCompteActif(
            CompteBancaire compte,
            String typeCompte
    ) {
        if (compte.getStatutCompte() != StatutCompte.ACTIF) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le compte " + typeCompte + " doit être actif"
            );
        }
    }

    private static void verifierDevise(
            CompteBancaire source,
            CompteBancaire destination
    ) {
        if (source.getDevise() != null
                && destination.getDevise() != null
                && !source.getDevise().equalsIgnoreCase(
                destination.getDevise()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les deux comptes doivent avoir la même devise"
            );
        }
    }

    private static String normaliserIdentifiant(String identifiant) {
        if (identifiant == null) {
            return "";
        }

        return identifiant
                .replaceAll("[\\s-]", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String nettoyerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            return "Virement";
        }

        String resultat = motif.trim();

        if (resultat.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le motif est trop long"
            );
        }

        return resultat;
    }
}