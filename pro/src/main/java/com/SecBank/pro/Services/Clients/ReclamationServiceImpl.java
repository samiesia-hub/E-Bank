package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Service
public class ReclamationServiceImpl implements ReclamationService {

    private static final int LONGUEUR_MIN_MESSAGE = 10;

    private final ClientRepo clientRepo;
    private final ReclamationRepo reclamationRepo;

    @Override
    @Transactional
    public Reclamation creerReclamation(String emailClient, String objet, String message) {
        Client client = chargerClient(emailClient);

        if (objet == null || objet.isBlank()) {
            throw new DonneeInvalideException("L'objet de la réclamation est obligatoire");
        }
        if (message == null || message.trim().length() < LONGUEUR_MIN_MESSAGE) {
            throw new DonneeInvalideException(
                    "Le message doit contenir au moins " + LONGUEUR_MIN_MESSAGE + " caractères");
        }

        Date maintenant = new Date();
        Reclamation reclamation = new Reclamation();
        reclamation.setObjet(objet.trim());
        reclamation.setDesciprtion(message.trim());
        reclamation.setStatus(StatusReclamation.EN_COURS);
        reclamation.setDateCreation(maintenant);
        reclamation.setDateMiseAJour(maintenant);
        reclamation.setClient(client);
        return reclamationRepo.save(reclamation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reclamation> listerReclamationsDuClient(String emailClient) {
        chargerClient(emailClient);
        return reclamationRepo.findByClientEmailOrderByDateCreationDesc(emailClient);
    }

    @Override
    @Transactional(readOnly = true)
    public Reclamation consulterReclamationDuClient(Long reclamationId, String emailClient) {
        Reclamation reclamation = chargerReclamation(reclamationId);
        boolean estProprietaire = reclamation.getClient() != null
                && emailClient != null
                && emailClient.equals(reclamation.getClient().getEmail());
        if (!estProprietaire) {
            throw new AccesRefuseException("Cette réclamation n'appartient pas au client connecté");
        }
        return reclamation;
    }

    @Override
    @Transactional
    public Reclamation changerStatut(Long reclamationId, StatusReclamation nouveauStatut) {
        if (nouveauStatut == null) {
            throw new DonneeInvalideException("Le nouveau statut est obligatoire");
        }
        Reclamation reclamation = chargerReclamation(reclamationId);
        StatusReclamation actuel = reclamation.getStatus();

        if (actuel == null || !actuel.peutPasserA(nouveauStatut)) {
            throw new DonneeInvalideException(
                    "Transition de statut interdite : " + actuel + " vers " + nouveauStatut);
        }

        reclamation.setStatus(nouveauStatut);
        reclamation.setDateMiseAJour(new Date());
        return reclamationRepo.save(reclamation);
    }

    private Client chargerClient(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email du client connecte est obligatoire");
        }
        return clientRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec email :" + email));
    }

    private Reclamation chargerReclamation(Long id) {
        return reclamationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamation introuvable avec id :" + id));
    }
}