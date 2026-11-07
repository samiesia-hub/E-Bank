package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Beneficiaire;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Repositories.Client.BeneficiaireRepository;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class BeneficiaireServiceImpl implements BeneficiaireService {

    private final BeneficiaireRepository beneficiaireRepository;
    private final ClientRepo clientRepo;

    public BeneficiaireServiceImpl(
            BeneficiaireRepository beneficiaireRepository,
            ClientRepo clientRepo
    ) {
        this.beneficiaireRepository = beneficiaireRepository;
        this.clientRepo = clientRepo;
    }

    @Override
    public List<Beneficiaire> listerBeneficiaires(String emailClient) {
        return beneficiaireRepository.trouverParEmailClient(emailClient);
    }

    @Override
    @Transactional
    public Beneficiaire creerBeneficiaire(
            String emailClient,
            String nom,
            String prenom,
            String numeroCompte
    ) {
        String nomValide = validerTexte(nom, "Le nom");
        String prenomValide = validerTexte(prenom, "Le prénom");
        String compteValide = validerRibOuIban(numeroCompte);

        if (beneficiaireRepository.existePourClient(emailClient, compteValide)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce bénéficiaire existe déjà dans votre liste"
            );
        }

        Client client = clientRepo.findByEmail(emailClient)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client introuvable"
                ));

        Beneficiaire beneficiaire = new Beneficiaire();
        beneficiaire.setNom(nomValide);
        beneficiaire.setPrenom(prenomValide);
        beneficiaire.setNumeroCompte(compteValide);
        beneficiaire.setClient(client);

        return beneficiaireRepository.save(beneficiaire);
    }

    @Override
    @Transactional
    public void supprimerBeneficiaire(
            String emailClient,
            Long beneficiaireId
    ) {
        Beneficiaire beneficiaire = beneficiaireRepository.findById(beneficiaireId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bénéficiaire introuvable"
                ));

        if (beneficiaire.getClient() == null
                || !emailClient.equalsIgnoreCase(
                        beneficiaire.getClient().getEmail()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez pas supprimer le bénéficiaire d'un autre client"
            );
        }

        beneficiaireRepository.delete(beneficiaire);
    }

    private static String validerTexte(String valeur, String nomChamp) {
        if (valeur == null || valeur.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    nomChamp + " est obligatoire"
            );
        }

        String resultat = valeur.trim();

        if (resultat.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    nomChamp + " est trop long"
            );
        }

        return resultat;
    }

    private static String validerRibOuIban(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le RIB ou l'IBAN est obligatoire"
            );
        }

        String numero = valeur
                .replaceAll("[\\s-]", "")
                .toUpperCase(Locale.ROOT);

        boolean ribMarocain = numero.matches("\\d{24}");
        boolean ibanMarocain = numero.matches("MA\\d{26}");

        if (!ribMarocain && !ibanMarocain) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "RIB invalide : 24 chiffres attendus, ou IBAN marocain MA suivi de 26 chiffres"
            );
        }

        return numero;
    }
}