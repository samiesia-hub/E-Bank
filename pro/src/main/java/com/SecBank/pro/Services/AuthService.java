package com.SecBank.pro.Services;

import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Models.Énums.Role;
import com.SecBank.pro.Repositories.UserRepo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Authentification des utilisateurs : connexion, inscription et déconnexion.
 */
@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean seConnecter(String email, String motDePasse) {
        if (email == null || motDePasse == null) {
            return false;
        }
        return userRepo.findByEmail(email)
                .filter(Utilisateur::isActif)
                .map(utilisateur -> passwordEncoder.matches(motDePasse, utilisateur.getPassword()))
                .orElse(false);
    }

    /**
     * Inscription d'un nouveau client. Lève IllegalArgumentException si l'email
     * est déjà utilisé ou si un champ obligatoire est vide.
     */
    public Utilisateur inscrire(String email, String motDePasse, String nom) {
        if (email == null || email.isBlank() || motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Email et mot de passe sont obligatoires");
        }
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        Client client = new Client();
        client.setEmail(email);
        client.setUsername(email);
        client.setNom(nom);
        client.setPassword(passwordEncoder.encode(motDePasse));
        client.setRole(Role.CLIENT);
        client.setActif(true);
        client.setNumeroClient("CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return userRepo.save(client);
    }

    public Optional<Utilisateur> trouverParEmail(String email) {
        return userRepo.findByEmail(email);
    }

    /**
     * Le frontend attend les rôles CLIENT, AGENT_SUPPORT et ADMIN dans le token.
     * L'énumération du domaine utilise encore SUPPORT pour l'agent.
     */
    public String rolePourToken(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null) {
            return "";
        }
        return utilisateur.getRole() == Role.SUPPORT
                ? "AGENT_SUPPORT"
                : utilisateur.getRole().name();
    }

    public void seDeconnecter() {
        // Token JWT sans session : la déconnexion consiste à vider le contexte courant.
        SecurityContextHolder.clearContext();
    }

    public void reinitialiserMotDePasse(String email) {
        // À brancher sur OtpService + un service d'envoi d'email (non fourni dans ce sprint).
        throw new UnsupportedOperationException("Réinitialisation par email non implémentée");
    }

    public void gererProfil() {
        // Le profil sera branché sur le controller client au prochain sprint.
        throw new UnsupportedOperationException("Gestion du profil non implémentée");
    }
}
