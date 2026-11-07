package com.SecBank.pro.Services.Clients;

import com.SecBank.pro.Dtos.ChangePasswordRequestDTO;
import com.SecBank.pro.Dtos.ModifierProfilRequest;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepo clientRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Client consulterProfil(String email) {
        return chargerClient(email);
    }

    @Override
    @Transactional
    public Client modifierProfil(String email, ModifierProfilRequest requete) {
        Client client = chargerClient(email);
        if (requete == null) {
            throw new DonneeInvalideException("Les données du profil sont obligatoires");
        }

        if (requete.nom() != null) {
            String nom = requete.nom().trim();
            if (nom.isEmpty()) {
                throw new DonneeInvalideException("Le nom ne peut pas être vide");
            }
            client.setNom(nom);
        }
        if (requete.telephone() != null) {
            client.setTelephone(convertirTelephone(requete.telephone()));
        }
        if (requete.adresse() != null) {
            client.setAddress(requete.adresse().trim());
        }
        return clientRepo.save(client);
    }

    @Override
    @Transactional
    public void changerMotDePasse(String email, ChangePasswordRequestDTO requete) {
        Client client = chargerClient(email);
        if (requete == null || estVide(requete.motDePasseActuel()) || estVide(requete.nouveauMotDePasse())) {
            throw new DonneeInvalideException("Le mot de passe actuel et le nouveau mot de passe sont obligatoires");
        }

        // On compare des empreintes BCrypt : on ne peut pas comparer deux textes.
        if (!passwordEncoder.matches(requete.motDePasseActuel(), client.getPassword())) {
            throw new DonneeInvalideException("Mot de passe actuel incorrect");
        }

        verifierRegleMotDePasse(requete.nouveauMotDePasse());
        client.setPassword(passwordEncoder.encode(requete.nouveauMotDePasse()));
        clientRepo.save(client);
    }

    private Client chargerClient(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email du client connecte est obligatoire");
        }
        return clientRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec email :" + email));
    }

    // Le téléphone est encore un int dans l'entité : on n'accepte que ce qui y tient.
    private int convertirTelephone(String saisie) {
        String chiffres = saisie.trim();
        if (!chiffres.matches("\\d{9,10}")) {
            throw new DonneeInvalideException("Numéro de téléphone invalide (9 ou 10 chiffres)");
        }
        try {
            return Integer.parseInt(chiffres);
        } catch (NumberFormatException e) {
            throw new DonneeInvalideException("Numéro de téléphone invalide (9 ou 10 chiffres)");
        }
    }

    // Même règle que le formulaire d'inscription du frontend, plus la limite de BCrypt (72 octets).
    private void verifierRegleMotDePasse(String motDePasse) {
        boolean conforme = motDePasse.length() >= 12
                && motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72
                && motDePasse.chars().anyMatch(Character::isUpperCase)
                && motDePasse.chars().anyMatch(Character::isLowerCase)
                && motDePasse.chars().anyMatch(Character::isDigit);
        if (!conforme) {
            throw new DonneeInvalideException(
                    "Le nouveau mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule et un chiffre");
        }
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}