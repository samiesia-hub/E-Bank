package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.Client.TransactionSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {

    /** Tri par defaut : les operations les plus recentes d'abord. */
    private static final Sort PLUS_RECENTES_D_ABORD = Sort.by(Sort.Direction.DESC, "dateTransaction")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final TransactionRepository transactionRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final ClientRepo clientRepo;

    @Override
    public List<Transaction> consulterHistorique(Long compteId) {
        return transactionRepository.findAll(TransactionSpecifications.parCompte(compteId), PLUS_RECENTES_D_ABORD);
    }

    @Override
    public List<Transaction> consulterTransactions() {
        return transactionRepository.findAll(PLUS_RECENTES_D_ABORD);
    }

    @Override
    public Page<Transaction> rechercherTransactionsDuCompte(Long compteId, String email,
                                                            LocalDate du, LocalDate au,
                                                            SensTransaction sens, Pageable pageable) {
        verifierProprietaire(compteId, email);
        return rechercher(compteId, null, du, au, sens, pageable);
    }

    @Override
    public Page<Transaction> rechercherTransactionsDuClient(String email, Long compteId,
                                                            LocalDate du, LocalDate au,
                                                            SensTransaction sens, Pageable pageable) {
        if (compteId != null) {
            // Un compte explicite passe par la meme verification d'appartenance.
            return rechercherTransactionsDuCompte(compteId, email, du, au, sens, pageable);
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email du client connecte est obligatoire");
        }
        Client client = clientRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec email :" + email));

        // Filtre sur le client : un client ne voit jamais les transactions d'un tiers.
        return rechercher(null, client.getId_Utilisateur(), du, au, sens, pageable);
    }

    @Override
    public Page<Transaction> rechercherToutesLesTransactions(Long clientId, Long compteId,
                                                             LocalDate du, LocalDate au,
                                                             SensTransaction sens, Pageable pageable) {
        return rechercher(compteId, clientId, du, au, sens, pageable);
    }

    private Page<Transaction> rechercher(Long compteId, Long clientId, LocalDate du, LocalDate au,
                                         SensTransaction sens, Pageable pageable) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new IllegalArgumentException("La date de debut doit preceder la date de fin");
        }

        Specification<Transaction> specification = TransactionSpecifications.parCompte(compteId)
                .and(TransactionSpecifications.parClient(clientId))
                .and(TransactionSpecifications.entreDates(du, au))
                .and(TransactionSpecifications.parSens(sens));

        Pageable pageableTrie = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), PLUS_RECENTES_D_ABORD)
                : pageable;

        return transactionRepository.findAll(specification, pageableTrie);
    }

    private void verifierProprietaire(Long compteId, String email) {
        CompteBancaire compte = compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec id :" + compteId));

        String proprietaire = compte.getClient() == null ? null : compte.getClient().getEmail();
        if (email == null || email.isBlank() || proprietaire == null || !proprietaire.equalsIgnoreCase(email)) {
            throw new AccesRefuseException("Ce compte n'appartient pas au client connecte");
        }
    }
}
