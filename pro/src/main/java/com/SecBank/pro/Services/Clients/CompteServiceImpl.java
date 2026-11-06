package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Exceptions.RibDejaUtiliseException;
import com.SecBank.pro.Exceptions.RibInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Models.Enums.TypeOperation;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.Admin.AuditService;
import com.SecBank.pro.Utils.IbanGenerator;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class CompteServiceImpl implements CompteService {

    private static final int MAX_TENTATIVES = 10;
    private static final int ECHELLE_SOLDE = 2;

    private final ClientRepo clientRepo;
    private final CompteBancaireRepository compteBancaireRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepo userRepo;
    private final AuditService auditService;

    @Override
    public BigDecimal consulterSolde(Long compteId) {
        return chargerCompte(compteId).getSolde();
    }

    @Override
    public List<CompteBancaire> consulterComptes(Long clientId) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id :" + clientId));
        return compteBancaireRepository.findByClientId(client.getId_Utilisateur());
    }

    @Override
    public List<CompteBancaire> consulterComptesDuClient(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email du client connecte est obligatoire");
        }
        Client client = clientRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec email :" + email));
        return compteBancaireRepository.findByClientId(client.getId_Utilisateur());
    }

    @Override
    public CompteBancaire consulterCompteDuClient(Long compteId, String email) {
        CompteBancaire compte = chargerCompte(compteId);
        String proprietaire = compte.getClient() == null ? null : compte.getClient().getEmail();

        if (email == null || email.isBlank() || proprietaire == null || !proprietaire.equalsIgnoreCase(email)) {
            // 404 volontaire : on ne revele pas l'existence d'un compte appartenant a un autre client.
            throw new ResourceNotFoundException("Compte introuvable avec id :" + compteId);
        }
        return compte;
    }

    @Override
    public CompteBancaire consulterCompte(Long compteId) {
        return chargerCompte(compteId);
    }

    @Override
    @Transactional
    public CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte) {
        return creerCompte(clientId, typeCompte, null, null);
    }

    @Override
    @Transactional
    public CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte, String rib) {
        return creerCompte(clientId, typeCompte, rib, null);
    }

    @Override
    @Transactional
    public CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte, String rib, BigDecimal soldeInitial) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire");
        }
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id :" + clientId));

        if (rib != null && !rib.isBlank()) {
            return creerCompteAvecRib(client, typeCompte, rib.trim(), soldeInitial);
        }

        for (int tentative = 0; tentative < MAX_TENTATIVES; tentative++) {
            String numeroCompte = IbanGenerator.genererNumeroCompte();
            String ribGenere = IbanGenerator.genererRib(numeroCompte);
            if (compteBancaireRepository.existsByNumeroCompte(numeroCompte)
                    || compteBancaireRepository.existsByRib(ribGenere)) {
                continue;
            }

            return enregistrerCompte(client, numeroCompte, ribGenere, typeCompte, soldeInitial);
        }

        throw new RibDejaUtiliseException(
                "Aucun RIB unique n'a pu etre genere apres " + MAX_TENTATIVES + " tentatives");
    }

    /** RIB saisi par un agent : format et cle de controle, puis unicite. */
    private CompteBancaire creerCompteAvecRib(Client client, TypeCompte typeCompte, String rib,
                                              BigDecimal soldeInitial) {
        String motif = IbanGenerator.raisonInvalidite(rib);
        if (motif != null) {
            throw new RibInvalideException(motif);
        }

        String numeroCompte = IbanGenerator.numeroCompteDepuisRib(rib);
        if (compteBancaireRepository.existsByRib(rib)) {
            throw new RibDejaUtiliseException("Un compte avec le RIB " + rib + " existe deja");
        }
        if (compteBancaireRepository.existsByNumeroCompte(numeroCompte)) {
            throw new RibDejaUtiliseException(
                    "Un compte avec le numero de compte " + numeroCompte + " existe deja");
        }

        return enregistrerCompte(client, numeroCompte, rib, typeCompte, soldeInitial);
    }

    @Override
    public List<CompteBancaire> listerTousLesComptes() {
        return compteBancaireRepository.findAllByOrderByDateCreationDesc();
    }

    @Override
    @Transactional
    public CompteBancaire changerStatut(Long compteId, StatutCompte statut) {
        if (statut == null) {
            throw new DonneeInvalideException("Le statut est obligatoire");
        }
        if (statut == StatutCompte.CLOTURE) {
            throw new DonneeInvalideException(
                    "La cloture n'est pas geree ici : seuls le gel et la reactivation sont possibles");
        }

        CompteBancaire compte = chargerCompte(compteId);
        if (compte.getStatutCompte() == StatutCompte.CLOTURE) {
            throw new DonneeInvalideException("Un compte cloture ne peut plus changer de statut");
        }
        if (compte.getStatutCompte() == statut) {
            return compte;
        }

        compte.setStatutCompte(statut);
        return compteBancaireRepository.save(compte);
    }

    private CompteBancaire enregistrerCompte(Client client, String numeroCompte, String rib,
                                             TypeCompte typeCompte, BigDecimal soldeInitial) {
        BigDecimal solde = soldeInitial == null ? BigDecimal.ZERO : soldeInitial;
        if (solde.signum() < 0) {
            throw new DonneeInvalideException("Le solde initial ne peut pas etre negatif");
        }

        CompteBancaire compte = new CompteBancaire();
        compte.setNumeroCompte(numeroCompte);
        compte.setRib(rib);
        compte.setDevise(DEVISE_PAR_DEFAUT);
        compte.setSolde(solde.setScale(ECHELLE_SOLDE, RoundingMode.HALF_UP));
        compte.setTypeCompte(typeCompte == null ? TypeCompte.COURANT : typeCompte);
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setDateCreation(LocalDateTime.now());
        compte.setClient(client);

        try {
            // Flush immediat : une violation de la contrainte d'unicite remonte ici
            // et devient un 409 explicite, jamais une erreur 500.
            return compteBancaireRepository.saveAndFlush(compte);
        } catch (DataIntegrityViolationException conflit) {
            throw new RibDejaUtiliseException("Un compte avec le RIB " + rib + " existe deja");
        }
    }

    @Override
    @Transactional
    public void debiter(Long compteId, BigDecimal montant) {
        verifierMontant(montant);
        CompteBancaire compte = chargerCompteOperable(compteId);
        retirerDuSolde(compte, montant);
    }

    @Override
    @Transactional
    public void crediter(Long compteId, BigDecimal montant) {
        verifierMontant(montant);
        CompteBancaire compte = chargerCompteOperable(compteId);
        ajouterAuSolde(compte.getId(), montant);
    }

    @Override
    @Transactional
    public OperationGuichet deposer(Long compteId, BigDecimal montant, String motif, String emailAgent) {
        return enregistrerOperation(TypeOperation.DEPOT, compteId, montant, motif, emailAgent);
    }

    @Override
    @Transactional
    public OperationGuichet retirer(Long compteId, BigDecimal montant, String motif, String emailAgent) {
        return enregistrerOperation(TypeOperation.RETRAIT, compteId, montant, motif, emailAgent);
    }

    /**
     * Coeur metier des operations de guichet : validation, mouvement de solde
     * atomique, transaction tracee puis entree d'audit.
     */
    private OperationGuichet enregistrerOperation(TypeOperation type, Long compteId, BigDecimal montant,
                                                  String motif, String emailAgent) {
        verifierMontant(montant);
        Utilisateur agent = chargerAgent(emailAgent);
        CompteBancaire compte = chargerCompteOperable(compteId);

        if (type == TypeOperation.DEPOT) {
            ajouterAuSolde(compte.getId(), montant);
        } else {
            retirerDuSolde(compte, montant);
        }

        // Solde relu apres la mise a jour atomique : la valeur en base fait foi.
        CompteBancaire compteAJour = chargerCompte(compteId);

        OperationGuichet operation = new OperationGuichet();
        operation.setTypeOperation(type);
        operation.setMotif(motif);
        operation.setAgent(agent);
        operation.setCompte(compteAJour);
        operation.setMontant(montant);
        operation.setSens(type == TypeOperation.DEPOT ? SensTransaction.CREDIT : SensTransaction.DEBIT);
        operation.setStatutTransaction(StatutTransaction.EXECUTEE);
        operation.setDateTransaction(LocalDateTime.now());

        OperationGuichet enregistree = transactionRepository.save(operation);
        journaliserOperation(enregistree, compteAJour, agent, motif);

        return enregistree;
    }

    private void journaliserOperation(OperationGuichet operation, CompteBancaire compte,
                                      Utilisateur agent, String motif) {
        String action = operation.getTypeOperation() == TypeOperation.DEPOT ? "DEPOT_GUICHET" : "RETRAIT_GUICHET";
        String devise = compte.getDevise() == null ? DEVISE_PAR_DEFAUT : compte.getDevise();

        String details = String.format(
                "%s de %s %s sur le compte %s (id %s) par l'agent %s - motif : %s",
                operation.getTypeOperation(),
                operation.getMontant().setScale(ECHELLE_SOLDE, RoundingMode.HALF_UP).toPlainString(),
                devise,
                compte.getNumeroCompte(),
                compte.getId(),
                agent.getEmail(),
                motif == null || motif.isBlank() ? "non renseigne" : motif);

        // Le journal conserve l'agent, le compte, le montant et l'horodatage.
        auditService.journaliser(action, agent, details);
    }

    private void ajouterAuSolde(Long compteId, BigDecimal montant) {
        int lignes = compteBancaireRepository.incrementerSolde(compteId, montant, StatutCompte.ACTIF);
        if (lignes != 1) {
            throw new OperationRefuseeException("Le compte " + compteId + " n'est pas actif : operation refusee");
        }
    }

    private void retirerDuSolde(CompteBancaire compte, BigDecimal montant) {
        if (montant.compareTo(compte.getSolde()) > 0) {
            throw new OperationRefuseeException(String.format(
                    "Solde insuffisant : solde %s, retrait demande %s",
                    compte.getSolde().toPlainString(), montant.toPlainString()));
        }

        int lignes = compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                compte.getId(), montant, StatutCompte.ACTIF);
        if (lignes != 1) {
            // Solde insuffisant ou compte devenu inoperable entre la lecture et l'ecriture.
            throw new OperationRefuseeException("Retrait refuse : solde insuffisant ou compte inactif");
        }
    }

    private static void verifierMontant(BigDecimal montant) {
        if (montant == null) {
            throw new OperationRefuseeException("Le montant est obligatoire");
        }
        if (montant.signum() <= 0) {
            throw new OperationRefuseeException("Le montant doit etre strictement positif");
        }
    }

    private Utilisateur chargerAgent(String emailAgent) {
        if (emailAgent == null || emailAgent.isBlank()) {
            throw new OperationRefuseeException("L'agent auteur de l'operation est obligatoire");
        }
        return userRepo.findByEmail(emailAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable avec email :" + emailAgent));
    }

    private CompteBancaire chargerCompteOperable(Long compteId) {
        CompteBancaire compte = chargerCompte(compteId);
        if (compte.getStatutCompte() != StatutCompte.ACTIF) {
            throw new OperationRefuseeException(
                    "Le compte " + compteId + " n'est pas actif (statut " + compte.getStatutCompte() + ")");
        }
        return compte;
    }

    @Override
    public void gererComptesBancaires() {
        throw new UnsupportedOperationException("Non implémenté");
    }

    private CompteBancaire chargerCompte(Long compteId) {
        return compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec id :" + compteId));
    }

}
