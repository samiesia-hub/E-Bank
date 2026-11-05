package com.SecBank.pro;

import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserRepo userRepo;

    private AuthService authService() {
        return new AuthService(userRepo, passwordEncoder);
    }

    @Test
    void connexionAccepteeAvecLeBonMotDePasse() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("sami@example.com");
        utilisateur.setPassword(passwordEncoder.encode("secret123"));
        utilisateur.setActif(true);
        given(userRepo.findByEmail("sami@example.com")).willReturn(Optional.of(utilisateur));

        assertTrue(authService().seConnecter("sami@example.com", "secret123"));
    }

    @Test
    void connexionRefuseeAvecUnMauvaisMotDePasse() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("sami@example.com");
        utilisateur.setPassword(passwordEncoder.encode("secret123"));
        given(userRepo.findByEmail("sami@example.com")).willReturn(Optional.of(utilisateur));

        assertFalse(authService().seConnecter("sami@example.com", "mauvais"));
    }

    @Test
    void connexionRefuseeSiEmailInconnu() {
        given(userRepo.findByEmail("inconnu@example.com")).willReturn(Optional.empty());

        assertFalse(authService().seConnecter("inconnu@example.com", "secret123"));
    }

    @Test
    void inscriptionEnregistreLeMotDePasseHacheEtLeRoleClient() {
        given(userRepo.findByEmail("sami@example.com")).willReturn(Optional.empty());
        given(userRepo.save(any(Utilisateur.class))).willAnswer(invocation -> invocation.getArgument(0));

        authService().inscrire("sami@example.com", "secret123", "Sami");

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(userRepo).save(captor.capture());
        Utilisateur enregistre = captor.getValue();

        assertEquals("sami@example.com", enregistre.getEmail());
        assertEquals("Sami", enregistre.getNom());
        assertTrue(passwordEncoder.matches("secret123", enregistre.getPassword()));
    }

    @Test
    void inscriptionRefuseeSiEmailDejaUtilise() {
        given(userRepo.findByEmail("sami@example.com")).willReturn(Optional.of(new Utilisateur()));

        assertThrows(IllegalArgumentException.class,
                () -> authService().inscrire("sami@example.com", "secret123", "Sami"));
    }

    @Test
    void inscriptionRefuseeSansMotDePasse() {
        assertThrows(IllegalArgumentException.class,
                () -> authService().inscrire("sami@example.com", "  ", "Sami"));
    }
}
