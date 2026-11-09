package com.SecBank.pro.Services;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReinitialisationMotDePasseServiceTest {

    private static final String EMAIL = "jean@example.com";

    @Mock
    private UserRepo userRepo;

    @Mock
    private OtpService otpService;

    @Mock
    private EnvoiCodeService envoiCodeService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private ReinitialisationMotDePasseService service;
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        service = new ReinitialisationMotDePasseService(userRepo, passwordEncoder, otpService, envoiCodeService);
        utilisateur = new Utilisateur();
        utilisateur.setEmail(EMAIL);
        utilisateur.setActif(true);
        utilisateur.setPassword(passwordEncoder.encode("AncienMotDePasse1"));
    }

    @Test
    void demanderReinitialisation_emailConnu_genereEtEnvoieLeCode() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(otpService.generer(EMAIL)).thenReturn("123456");

        service.demanderReinitialisation(EMAIL);

        verify(envoiCodeService).envoyerCodeReinitialisation(EMAIL, "123456");
    }

    @Test
    void demanderReinitialisation_emailInconnu_neFaitRienEtNeLevePasDErreur() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        service.demanderReinitialisation("inconnu@example.com");

        verify(otpService, never()).generer(any());
        verify(envoiCodeService, never()).envoyerCodeReinitialisation(any(), any());
    }

    @Test
    void demanderReinitialisation_compteInactif_neGenerePasDeCode() {
        utilisateur.setActif(false);
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));

        service.demanderReinitialisation(EMAIL);

        verify(otpService, never()).generer(any());
    }

    @Test
    void reinitialiser_codeCorrect_metAJourLeHash() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(otpService.verifier(EMAIL, "123456")).thenReturn(true);

        service.reinitialiserMotDePasse(EMAIL, "123456", "NouveauMotDePasse2");

        assertTrue(passwordEncoder.matches("NouveauMotDePasse2", utilisateur.getPassword()));
        verify(userRepo).save(utilisateur);
    }

    @Test
    void reinitialiser_mauvaisCode_leveDonneeInvalide() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(otpService.verifier(EMAIL, "000000")).thenReturn(false);

        assertThrows(DonneeInvalideException.class, () ->
                service.reinitialiserMotDePasse(EMAIL, "000000", "NouveauMotDePasse2"));

        verify(userRepo, never()).save(any());
    }

    @Test
    void reinitialiser_emailInconnu_donneLeMemeMessageQueMauvaisCode() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        DonneeInvalideException erreur = assertThrows(DonneeInvalideException.class, () ->
                service.reinitialiserMotDePasse("inconnu@example.com", "123456", "NouveauMotDePasse2"));

        assertEquals("Code invalide ou expiré", erreur.getMessage());
    }

    @Test
    void reinitialiser_motDePasseFaible_neConsommePasLeCode() {
        assertThrows(DonneeInvalideException.class, () ->
                service.reinitialiserMotDePasse(EMAIL, "123456", "court"));

        verify(otpService, never()).verifier(any(), any());
    }
}
