package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.DossierKYC;
import com.SecBank.pro.Models.Enums.StatutKYC;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Client.DossierKYCRepository;
import com.SecBank.pro.Repositories.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock
    private DossierKYCRepository dossierKYCRepository;

    @Mock
    private UserRepo userRepo;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private KycServiceImpl kycService;

    @Test
    @DisplayName("Given un dossier en attente, When un agent le valide, Then le statut passe a VALIDE et l'audit est ecrit")
    void validerDossierPasseLeStatutAValide() {
        DossierKYC dossier = dossier(StatutKYC.EN_ATTENTE);
        Utilisateur agent = agent();

        when(dossierKYCRepository.findById(4L)).thenReturn(Optional.of(dossier));
        when(userRepo.findByEmail("agent@example.com")).thenReturn(Optional.of(agent));
        when(dossierKYCRepository.save(any(DossierKYC.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DossierKYC resultat = kycService.validerDossierKYC(4L, "agent@example.com", "127.0.0.1");

        assertEquals(StatutKYC.VALIDE, resultat.getStatutKYC());
        verify(auditService).journaliser(eq("KYC_VALIDATION"), eq(agent), contains("Dossier KYC 4"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Given un refus sans motif, When l'agent rejette, Then 400 et le dossier reste EN_ATTENTE")
    void rejetSansMotifEstRefuse() {
        DossierKYC dossier = dossier(StatutKYC.EN_ATTENTE);

        assertThrows(DonneeInvalideException.class,
                () -> kycService.rejeterDossierKYC(4L, "  ", "agent@example.com", "127.0.0.1"));

        assertEquals(StatutKYC.EN_ATTENTE, dossier.getStatutKYC());
        verifyNoInteractions(dossierKYCRepository, auditService);
    }

    @Test
    @DisplayName("Given un motif, When l'agent rejette, Then le statut passe a REJETE et le motif est journalise")
    void rejetAvecMotifJournaliseLaDecision() {
        DossierKYC dossier = dossier(StatutKYC.EN_ATTENTE);
        Utilisateur agent = agent();

        when(dossierKYCRepository.findById(4L)).thenReturn(Optional.of(dossier));
        when(userRepo.findByEmail("agent@example.com")).thenReturn(Optional.of(agent));
        when(dossierKYCRepository.save(any(DossierKYC.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DossierKYC resultat = kycService.rejeterDossierKYC(4L, "Piece illisible", "agent@example.com", null);

        assertEquals(StatutKYC.REJETE, resultat.getStatutKYC());
        verify(auditService).journaliser(eq("KYC_REFUS"), eq(agent), contains("Piece illisible"), any());
    }

    @Test
    @DisplayName("Given un dossier deja traite, When une nouvelle decision arrive, Then 400")
    void dossierDejaTraiteEstRefuse() {
        when(dossierKYCRepository.findById(4L)).thenReturn(Optional.of(dossier(StatutKYC.VALIDE)));

        assertThrows(DonneeInvalideException.class,
                () -> kycService.validerDossierKYC(4L, "agent@example.com", null));
    }

    @Test
    @DisplayName("Given un dossier inconnu, When une decision arrive, Then 404")
    void dossierInconnuRenvoie404() {
        when(dossierKYCRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> kycService.validerDossierKYC(99L, "agent@example.com", null));
    }

    @Test
    @DisplayName("Given des dossiers en attente, When le support liste, Then seuls les EN_ATTENTE sont demandes")
    void listerDossiersEnAttente() {
        when(dossierKYCRepository.findByStatutKYCOrderByDateSoumissionAsc(StatutKYC.EN_ATTENTE))
                .thenReturn(List.of(dossier(StatutKYC.EN_ATTENTE)));

        assertEquals(1, kycService.listerDossiersEnAttente().size());
        verify(dossierKYCRepository).findByStatutKYCOrderByDateSoumissionAsc(StatutKYC.EN_ATTENTE);
    }

    private static DossierKYC dossier(StatutKYC statut) {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");
        client.setNom("Jean");

        DossierKYC dossier = new DossierKYC();
        dossier.setId(4L);
        dossier.setStatutKYC(statut);
        dossier.setDateSoumission(LocalDateTime.now().minusDays(2));
        dossier.setClient(client);
        return dossier;
    }

    private static Utilisateur agent() {
        Utilisateur agent = new Utilisateur();
        agent.setId_Utilisateur(7L);
        agent.setEmail("agent@example.com");
        return agent;
    }
}
