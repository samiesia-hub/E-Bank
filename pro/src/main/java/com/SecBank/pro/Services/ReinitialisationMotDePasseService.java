package com.SecBank.pro.Services;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Utils.RegleMotDePasse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReinitialisationMotDePasseService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EnvoiCodeService envoiCodeService;

    public ReinitialisationMotDePasseService(UserRepo userRepo,
                                             PasswordEncoder passwordEncoder,
                                             OtpService otpService,
                                             EnvoiCodeService envoiCodeService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.envoiCodeService = envoiCodeService;
    }

    /**
     * Génère un code et l'envoie, si le compte existe et est actif.
     * Ne signale JAMAIS si l'email existe : sinon on révèle quels comptes existent.
     */
    public void demanderReinitialisation(String email) {
        if (email == null || email.isBlank()) {
            throw new DonneeInvalideException("L'email est obligatoire");
        }
        userRepo.findByEmail(email.trim())
                .filter(Utilisateur::isActif)
                .ifPresent(utilisateur -> {
                    String code = otpService.generer(utilisateur.getEmail());
                    envoiCodeService.envoyerCodeReinitialisation(utilisateur.getEmail(), code);
                });
    }

    @Transactional
    public void reinitialiserMotDePasse(String email, String code, String nouveauMotDePasse) {
        if (estVide(email) || estVide(code) || estVide(nouveauMotDePasse)) {
            throw new DonneeInvalideException("L'email, le code et le nouveau mot de passe sont obligatoires");
        }

        // La règle est vérifiée AVANT le code : un mot de passe refusé ne doit pas brûler le code.
        RegleMotDePasse.verifier(nouveauMotDePasse);

        Utilisateur utilisateur = userRepo.findByEmail(email.trim())
                .filter(Utilisateur::isActif)
                .orElse(null);

        // Même message pour un email inconnu et pour un mauvais code.
        if (utilisateur == null || !otpService.verifier(utilisateur.getEmail(), code.trim())) {
            throw new DonneeInvalideException("Code invalide ou expiré");
        }

        utilisateur.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        userRepo.save(utilisateur);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
