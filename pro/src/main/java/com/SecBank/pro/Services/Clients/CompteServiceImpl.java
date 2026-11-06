package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class CompteServiceImpl implements CompteService {

    /** Code banque, ville et agence utilises pour construire le RIB. */
    private static final String PREFIXE_BANQUE = "011";
    private static final String CODE_VILLE = "780";
    private static final String CODE_AGENCE = "001";
    private static final int LONGUEUR_NUMERO_COMPTE = 13;
    private static final int LONGUEUR_RIB = 24;
    private static final int MAX_TENTATIVES = 10;
    private static final int ECHELLE_SOLDE = 2;
    private static final BigInteger MODULO_RIB = BigInteger.valueOf(97);
    private static final BigInteger CENT = BigInteger.valueOf(100);
    private static final SecureRandom ALEATOIRE = new SecureRandom();

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
    public CompteBancaire creerCompte(Long clientId, TypeCompte typeCompte) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire");
        }
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id :" + clientId));

        for (int tentative = 0; tentative < MAX_TENTATIVES; tentative++) {
            String numeroCompte = genererNumeroCompte();
            String rib = genererRib(numeroCompte);
            if (compteBancaireRepository.existsByNumeroCompte(numeroCompte)
                    || compteBancaireRepository.existsByRib(rib)) {
                continue;
            }

            CompteBancaire compte = new CompteBancaire();
            compte.setNumeroCompte(numeroCompte);
            compte.setRib(rib);
            compte.setDevise(DEVISE_PAR_DEFAUT);
            compte.setSolde(BigDecimal.ZERO.setScale(ECHELLE_SOLDE, RoundingMode.HALF_UP));
            compte.setTypeCompte(typeCompte == null ? TypeCompte.COURANT : typeCompte);
            compte.setStatutCompte(StatutCompte.ACTIF);
            compte.setDateCreation(LocalDateTime.now());
            compte.setClient(client);

            return compteBancaireRepository.save(compte);
        }

        throw new IllegalStateException("Impossible de generer un numero de compte / RIB unique");
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

    /** Numero de compte sur 13 chiffres, tire aleatoirement puis verifie en base. */
    private static String genererNumeroCompte() {
        long valeur = ALEATOIRE.nextLong(1_000_000_000_000L);
        return String.format("%0" + LONGUEUR_NUMERO_COMPTE + "d", valeur);
    }

    /**
     * RIB = banque(3) + ville(3) + agence(3) + numero de compte(13) + cle(2).
     * La cle est calculee pour que le RIB complet soit divisible par 97.
     */
    private static String genererRib(String numeroCompte) {
        String base = PREFIXE_BANQUE + CODE_VILLE + CODE_AGENCE + numeroCompte;
        int reste = new BigInteger(base).multiply(CENT).mod(MODULO_RIB).intValue();
        int cle = (MODULO_RIB.intValue() - reste) % MODULO_RIB.intValue();

        String rib = base + String.format("%02d", cle);
        if (rib.length() != LONGUEUR_RIB) {
            throw new IllegalStateException("RIB genere invalide : " + rib);
        }
        return rib;
    }
}
