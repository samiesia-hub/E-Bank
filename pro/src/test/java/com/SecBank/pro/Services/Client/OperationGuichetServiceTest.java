package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeOperation;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Repositories.UserRepo;
import com.SecBank.pro.Services.Admin.AuditService;
import com.SecBank.pro.Services.Clients.CompteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationGuichetServiceTest {

    private static final String EMAIL_AGENT = "admin@example.com";
    private static final String MOTIF = "Versement especes";

    @Mock
    private ClientRepo clientRepo;

    @Mock
    private CompteBancaireRepository compteBancaireRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepo userRepo;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CompteServiceImpl compteService;

    private CompteBancaire compte;
    private Utilisateur agent;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");

        compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setDevise("MAD");
        compte.setSolde(new BigDecimal("1500.00"));
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setClient(client);

        agent = new Utilisateur();
        agent.setId_Utilisateur(5L);
        agent.setEmail(EMAIL_AGENT);
    }

    @Test
    void depot500_augmenteLeSoldeEtCreeUneTransactionDepot() {

        // Arrange : solde avant 1500.00, solde relu apres la mise a jour 2000.00
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.of(agent));
        when(compteBancaireRepository.findById(10L))
                .thenReturn(Optional.of(compte))
                .thenReturn(Optional.of(compteAvecSolde("2000.00")));
        when(compteBancaireRepository.incrementerSolde(eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF)))
                .thenReturn(1);
        when(transactionRepository.save(any(OperationGuichet.class))).thenAnswer(invocation -> {
            OperationGuichet enregistree = invocation.getArgument(0);
            enregistree.setId(99L);
            return enregistree;
        });

        // Act
        OperationGuichet operation = compteService.deposer(
                10L, new BigDecimal("500.00"), MOTIF, EMAIL_AGENT);

        // Assert : la transaction tracee
        assertEquals(99L, operation.getId());
        assertEquals(TypeOperation.DEPOT, operation.getTypeOperation());
        assertEquals(SensTransaction.CREDIT, operation.getSens());
        assertEquals(StatutTransaction.EXECUTEE, operation.getStatutTransaction());
        assertEquals(new BigDecimal("500.00"), operation.getMontant());
        assertEquals(MOTIF, operation.getMotif());
        assertEquals(agent, operation.getAgent());
        assertNotNull(operation.getDateTransaction());
        assertEquals(new BigDecimal("2000.00"), operation.getCompte().getSolde());

        // Assert : le solde augmente atomiquement une seule fois
        verify(compteBancaireRepository, times(1))
                .incrementerSolde(eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF));
        verify(compteBancaireRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void depot_journaliseLAuditAvecAgentCompteMontantEtHorodatage() {

        // Arrange
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.of(agent));
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compteAvecSolde("2000.00")));
        when(compteBancaireRepository.incrementerSolde(eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF)))
                .thenReturn(1);
        when(transactionRepository.save(any(OperationGuichet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        compteService.deposer(10L, new BigDecimal("500.00"), MOTIF, EMAIL_AGENT);

        // Assert : action, agent, compte et montant dans l'entree d'audit
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).journaliser(eq("DEPOT_GUICHET"), eq(agent), details.capture());

        String contenu = details.getValue();
        assertTrue(contenu.contains("500.00"), contenu);
        assertTrue(contenu.contains("0000000000010"), contenu);
        assertTrue(contenu.contains(EMAIL_AGENT), contenu);
        assertTrue(contenu.contains("MAD"), contenu);
    }

    @Test
    void retrait500_surCompteA200_estRefuseEtLeSoldeResteInchange() {

        // Arrange
        compte.setSolde(new BigDecimal("200.00"));
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.of(agent));
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));

        // Act + Assert
        OperationRefuseeException refus = assertThrows(OperationRefuseeException.class, () ->
                compteService.retirer(10L, new BigDecimal("500.00"), "Retrait especes", EMAIL_AGENT));

        assertTrue(refus.getMessage().contains("Solde insuffisant"), refus.getMessage());
        assertEquals(new BigDecimal("200.00"), compte.getSolde());

        verify(compteBancaireRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
        verify(compteBancaireRepository, never()).incrementerSolde(any(), any(), any());
        verify(transactionRepository, never()).save(any(OperationGuichet.class));
        verify(auditService, never()).journaliser(anyString(), any(), anyString());
    }

    @Test
    void retrait_soldeSuffisant_debiteEtTraceUneTransactionRetrait() {

        // Arrange
        compte.setSolde(new BigDecimal("500.00"));
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.of(agent));
        when(compteBancaireRepository.findById(10L))
                .thenReturn(Optional.of(compte))
                .thenReturn(Optional.of(compteAvecSolde("300.00")));
        when(compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF))).thenReturn(1);
        when(transactionRepository.save(any(OperationGuichet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OperationGuichet operation = compteService.retirer(
                10L, new BigDecimal("200.00"), "Retrait especes", EMAIL_AGENT);

        // Assert
        assertEquals(TypeOperation.RETRAIT, operation.getTypeOperation());
        assertEquals(SensTransaction.DEBIT, operation.getSens());
        assertEquals(new BigDecimal("300.00"), operation.getCompte().getSolde());
        verify(auditService, times(1)).journaliser(eq("RETRAIT_GUICHET"), eq(agent), anyString());
    }

    @Test
    void montantNulOuNegatif_estRefuseSansToucherAuCompte() {

        // Act + Assert : aucun acces base, le montant est invalide
        assertThrows(OperationRefuseeException.class, () ->
                compteService.deposer(10L, BigDecimal.ZERO, MOTIF, EMAIL_AGENT));
        assertThrows(OperationRefuseeException.class, () ->
                compteService.deposer(10L, new BigDecimal("-1.00"), MOTIF, EMAIL_AGENT));
        assertThrows(OperationRefuseeException.class, () ->
                compteService.retirer(10L, new BigDecimal("-50.00"), MOTIF, EMAIL_AGENT));
        assertThrows(OperationRefuseeException.class, () ->
                compteService.deposer(10L, null, MOTIF, EMAIL_AGENT));

        verify(compteBancaireRepository, never()).findById(any());
        verify(compteBancaireRepository, never()).incrementerSolde(any(), any(), any());
        verify(transactionRepository, never()).save(any(OperationGuichet.class));
        verify(auditService, never()).journaliser(anyString(), any(), anyString());
    }

    @Test
    void compteNonActif_refuseLOperation() {

        // Arrange
        compte.setStatutCompte(StatutCompte.BLOQUE);
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.of(agent));
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));

        // Act + Assert
        assertThrows(OperationRefuseeException.class, () ->
                compteService.deposer(10L, new BigDecimal("500.00"), MOTIF, EMAIL_AGENT));

        verify(compteBancaireRepository, never()).incrementerSolde(any(), any(), any());
        verify(transactionRepository, never()).save(any(OperationGuichet.class));
    }

    @Test
    void agentInconnu_refuseLOperation() {

        // Arrange
        when(userRepo.findByEmail(EMAIL_AGENT)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                compteService.deposer(10L, new BigDecimal("500.00"), MOTIF, EMAIL_AGENT));

        verify(compteBancaireRepository, never()).findById(any());
    }

    @Test
    void depot_metAjouteLeSoldeEtRetraitRetireLeSolde() {

        // Arrange
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));
        when(compteBancaireRepository.incrementerSolde(eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF)))
                .thenReturn(1);
        when(compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF))).thenReturn(1);

        // Act
        compteService.crediter(10L, new BigDecimal("100.00"));
        compteService.debiter(10L, new BigDecimal("40.00"));

        // Assert
        verify(compteBancaireRepository, times(1))
                .incrementerSolde(eq(10L), eq(new BigDecimal("100.00")), eq(StatutCompte.ACTIF));
        verify(compteBancaireRepository, times(1))
                .decrementerSoldeSiSoldeSuffisant(eq(10L), eq(new BigDecimal("40.00")), eq(StatutCompte.ACTIF));
    }

    @Test
    void debitRefuseParLaBase_remonteUneOperationRefusee() {

        // Arrange : la condition SQL a echoue (retrait concurrent)
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));
        when(compteBancaireRepository.decrementerSoldeSiSoldeSuffisant(
                eq(10L), any(BigDecimal.class), eq(StatutCompte.ACTIF))).thenReturn(0);

        // Act + Assert
        OperationRefuseeException refus = assertThrows(OperationRefuseeException.class, () ->
                compteService.debiter(10L, new BigDecimal("100.00")));
        assertTrue(refus.getMessage().contains("solde insuffisant"), refus.getMessage());
    }

    private CompteBancaire compteAvecSolde(String solde) {
        CompteBancaire copie = new CompteBancaire();
        copie.setId(compte.getId());
        copie.setNumeroCompte(compte.getNumeroCompte());
        copie.setDevise(compte.getDevise());
        copie.setStatutCompte(compte.getStatutCompte());
        copie.setClient(compte.getClient());
        copie.setSolde(new BigDecimal(solde));
        return copie;
    }
}
