package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Enums.StatusReclamation;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import com.SecBank.pro.Services.Clients.ReclamationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReclamationServiceTest {

    private static final String EMAIL = "jean@example.com";

    @Mock
    private ClientRepo clientRepo;

    @Mock
    private ReclamationRepo reclamationRepo;

    @InjectMocks
    private ReclamationServiceImpl reclamationService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setEmail(EMAIL);
    }

    // ---------- création ----------

    @Test
    void creerReclamation_valide_enregistreEnCoursAvecHorodatage() {
        when(clientRepo.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(reclamationRepo.save(any(Reclamation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reclamation resultat = reclamationService.creerReclamation(
                EMAIL, "Problème de virement", "Mon virement n'a pas été effectué.");

        assertEquals(StatusReclamation.EN_COURS, resultat.getStatus());
        assertEquals("Problème de virement", resultat.getObjet());
        assertEquals("Mon virement n'a pas été effectué.", resultat.getDesciprtion());
        assertSame(client, resultat.getClient());
        assertNotNull(resultat.getDateCreation());
        assertNotNull(resultat.getDateMiseAJour());
    }

    @Test
    void creerReclamation_objetVide_leveDonneeInvalide() {
        when(clientRepo.findByEmail(EMAIL)).thenReturn(Optional.of(client));

        assertThrows(DonneeInvalideException.class, () ->
                reclamationService.creerReclamation(EMAIL, "   ", "Un message assez long"));

        verify(reclamationRepo, never()).save(any());
    }

    @Test
    void creerReclamation_messageTropCourt_leveDonneeInvalide() {
        when(clientRepo.findByEmail(EMAIL)).thenReturn(Optional.of(client));

        assertThrows(DonneeInvalideException.class, () ->
                reclamationService.creerReclamation(EMAIL, "Objet", "court"));

        verify(reclamationRepo, never()).save(any());
    }

    @Test
    void creerReclamation_clientIntrouvable_leveResourceNotFound() {
        when(clientRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reclamationService.creerReclamation(EMAIL, "Objet", "Un message assez long"));

        verify(reclamationRepo, never()).save(any());
    }

    // ---------- lecture : isolation par client ----------

    @Test
    void listerReclamationsDuClient_neRetourneQueLesSiennes() {
        when(clientRepo.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(reclamationRepo.findByClientEmailOrderByDateCreationDesc(EMAIL))
                .thenReturn(List.of(reclamation(1L, client, StatusReclamation.EN_COURS)));

        List<Reclamation> resultat = reclamationService.listerReclamationsDuClient(EMAIL);

        assertEquals(1, resultat.size());
        verify(reclamationRepo).findByClientEmailOrderByDateCreationDesc(EMAIL);
        verify(reclamationRepo, never()).findAll();
    }

    @Test
    void consulterReclamationDuClient_proprietaire_retourneLaReclamation() {
        Reclamation reclamation = reclamation(1L, client, StatusReclamation.EN_COURS);
        when(reclamationRepo.findById(1L)).thenReturn(Optional.of(reclamation));

        assertSame(reclamation, reclamationService.consulterReclamationDuClient(1L, EMAIL));
    }

    @Test
    void consulterReclamationDuClient_autreClient_leveAccesRefuse() {
        Client autre = new Client();
        autre.setEmail("autre@example.com");
        when(reclamationRepo.findById(1L))
                .thenReturn(Optional.of(reclamation(1L, autre, StatusReclamation.EN_COURS)));

        assertThrows(AccesRefuseException.class, () ->
                reclamationService.consulterReclamationDuClient(1L, EMAIL));
    }

    @Test
    void consulterReclamationDuClient_introuvable_leveResourceNotFound() {
        when(reclamationRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reclamationService.consulterReclamationDuClient(99L, EMAIL));
    }

    // ---------- statuts et transitions ----------

    @Test
    void changerStatut_transitionAutorisee_metAJourLeStatutEtLaDate() {
        Reclamation reclamation = reclamation(1L, client, StatusReclamation.EN_COURS);
        when(reclamationRepo.findById(1L)).thenReturn(Optional.of(reclamation));
        when(reclamationRepo.save(any(Reclamation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reclamation resultat = reclamationService.changerStatut(1L, StatusReclamation.RESOLUE);

        assertEquals(StatusReclamation.RESOLUE, resultat.getStatus());
        assertNotNull(resultat.getDateMiseAJour());
    }

    @Test
    void changerStatut_transitionInterdite_leveDonneeInvalide() {
        Reclamation reclamation = reclamation(1L, client, StatusReclamation.FERMEE);
        when(reclamationRepo.findById(1L)).thenReturn(Optional.of(reclamation));

        assertThrows(DonneeInvalideException.class, () ->
                reclamationService.changerStatut(1L, StatusReclamation.EN_COURS));

        verify(reclamationRepo, never()).save(any());
    }

    @Test
    void peutPasserA_respecteLesTransitionsAutorisees() {
        assertTrue(StatusReclamation.EN_COURS.peutPasserA(StatusReclamation.RESOLUE));
        assertTrue(StatusReclamation.EN_COURS.peutPasserA(StatusReclamation.FERMEE));
        assertTrue(StatusReclamation.RESOLUE.peutPasserA(StatusReclamation.FERMEE));
        assertFalse(StatusReclamation.RESOLUE.peutPasserA(StatusReclamation.EN_COURS));
        assertFalse(StatusReclamation.FERMEE.peutPasserA(StatusReclamation.EN_COURS));
        assertFalse(StatusReclamation.EN_COURS.peutPasserA(StatusReclamation.EN_COURS));
    }

    private static Reclamation reclamation(Long id, Client proprietaire, StatusReclamation statut) {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(id);
        reclamation.setClient(proprietaire);
        reclamation.setStatus(statut);
        return reclamation;
    }
}