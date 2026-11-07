package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Dtos.ChangePasswordRequestDTO;
import com.SecBank.pro.Dtos.ModifierProfilRequest;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Services.Clients.ClientServiceImpl;
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
class ClientServiceTest {

    @Mock
    private ClientRepo clientRepo;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private ClientServiceImpl clientService;
    private Client client;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(clientRepo, passwordEncoder);
        client = new Client();
        client.setEmail("jean@example.com");
        client.setPassword(passwordEncoder.encode("AncienMotDePasse1"));
        when(clientRepo.findByEmail("jean@example.com")).thenReturn(Optional.of(client));
    }

    @Test
    void changerMotDePasse_ancienCorrect_enregistreLeNouveauHash() {
        clientService.changerMotDePasse("jean@example.com",
                new ChangePasswordRequestDTO("AncienMotDePasse1", "NouveauMotDePasse2"));

        assertTrue(passwordEncoder.matches("NouveauMotDePasse2", client.getPassword()));
        verify(clientRepo).save(client);
    }

    @Test
    void changerMotDePasse_ancienFaux_leveDonneeInvalide() {
        assertThrows(DonneeInvalideException.class, () -> clientService.changerMotDePasse(
                "jean@example.com", new ChangePasswordRequestDTO("Mauvais", "NouveauMotDePasse2")));

        verify(clientRepo, never()).save(any());
    }

    @Test
    void changerMotDePasse_nouveauNonConforme_leveDonneeInvalide() {
        assertThrows(DonneeInvalideException.class, () -> clientService.changerMotDePasse(
                "jean@example.com", new ChangePasswordRequestDTO("AncienMotDePasse1", "court")));

        verify(clientRepo, never()).save(any());
    }

    @Test
    void modifierProfil_metAJourNomTelephoneEtAdresse() {
        when(clientRepo.save(client)).thenReturn(client);

        Client modifie = clientService.modifierProfil("jean@example.com",
                new ModifierProfilRequest("Jean Dupont", "0612345678", "12 rue des Lilas"));

        assertEquals("Jean Dupont", modifie.getNom());
        assertEquals(612345678, modifie.getTelephone());
        assertEquals("12 rue des Lilas", modifie.getAddress());
    }

    @Test
    void modifierProfil_telephoneInvalide_leveDonneeInvalide() {
        assertThrows(DonneeInvalideException.class, () -> clientService.modifierProfil(
                "jean@example.com", new ModifierProfilRequest(null, "abc", null)));

        verify(clientRepo, never()).save(any());
    }
}