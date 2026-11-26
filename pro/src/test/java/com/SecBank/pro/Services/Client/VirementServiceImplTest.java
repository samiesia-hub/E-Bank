package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.InsufficientFundsException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Beneficiaire;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Repositories.Client.BeneficiaireRepository;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.VirementRepository;
import com.SecBank.pro.Services.Clients.VirementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirementServiceImplTest {

    private static final String EMAIL_SOURCE = "jean@example.com";
    private static final String NUMERO_DESTINATAIRE = "0000000000002";

    @Mock
    private CompteBancaireRepository compteRepository;

    @Mock
    private BeneficiaireRepository beneficiaireRepository;

    @Mock
    private VirementRepository virementRepository;

    @InjectMocks
    private VirementServiceImpl service;

    private Client clientSource;
    private Client clientDestinataire;
    private CompteBancaire compteSource;
    private CompteBancaire compteDestinataire;
    private Beneficiaire beneficiaire;
    private BigDecimal montant;

    @BeforeEach
    void setUp() {
        clientSource = new Client();
        clientSource.setId_Utilisateur(1L);
        clientSource.setEmail(EMAIL_SOURCE);
        clientSource.setNom("Dupont");

        clientDestinataire = new Client();
        clientDestinataire.setId_Utilisateur(2L);
        clientDestinataire.setEmail("sara@example.com");
        clientDestinataire.setNom("Sara");

        compteSource = new CompteBancaire();
        compteSource.setId(1L);
        compteSource.setNumeroCompte("0000000000001");
        compteSource.setStatutCompte(StatutCompte.ACTIF);
        compteSource.setDevise("MAD");
        compteSource.setClient(clientSource);

        compteDestinataire = new CompteBancaire();
        compteDestinataire.setId(2L);
        compteDestinataire.setNumeroCompte(NUMERO_DESTINATAIRE);
        compteDestinataire.setStatutCompte(StatutCompte.ACTIF);
        compteDestinataire.setDevise("MAD");
        compteDestinataire.setClient(clientDestinataire);

        beneficiaire = new Beneficiaire();
        beneficiaire.setId(5L);
        beneficiaire.setNom("Sara");
        beneficiaire.setPrenom("Alami");
        beneficiaire.setNumeroCompte(NUMERO_DESTINATAIRE);
        beneficiaire.setClient(clientSource);

        montant = new BigDecimal("150.00");
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private static void assertReponse(HttpStatus attendu, Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(attendu.value(), ex.getStatusCode().value());
    }

    /** Debit et credit acceptes, puis relecture des deux comptes pour la trace. */
    private void stubDebitEtCreditReussis() {
        when(compteRepository.decrementerSoldeSiSoldeSuffisant(1L, montant, StatutCompte.ACTIF))
                .thenReturn(1);
        when(compteRepository.incrementerSolde(2L, montant, StatutCompte.ACTIF))
                .thenReturn(1);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(compteRepository.findById(2L)).thenReturn(Optional.of(compteDestinataire));
    }

    /** save() renvoie l'objet recu, avec un id attribue s'il n'en a pas. */
    private void stubSave() {
        when(virementRepository.save(any(Virement.class))).thenAnswer(invocation -> {
            Virement v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(100L);
            }
            return v;
        });
    }

    // ------------------------------------------------------------------
    // effectuerVirement : cas nominal
    // ------------------------------------------------------------------

    @Test
    void effectuerVirement_succes_executeLeDebitEtLeCredit() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteDestinataire));
        stubDebitEtCreditReussis();
        stubSave();

        Virement resultat = service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "Loyer");

        assertEquals(SensTransaction.DEBIT, resultat.getSens());
        assertEquals(StatutTransaction.EXECUTEE, resultat.getStatutTransaction());
        assertEquals(montant, resultat.getMontant());
        assertEquals("Sara Alami", resultat.getBeneficiaire());
        assertEquals("Loyer", resultat.getMotif());
        assertTrue(resultat.getReference().matches("VIR-\\d{8}-000100"),
                "la reference doit suivre le format VIR-AAAAMMJJ-NNNNNN");

        ArgumentCaptor<Virement> captor = ArgumentCaptor.forClass(Virement.class);
        verify(virementRepository, times(3)).save(captor.capture());
        Virement ligneCredit = captor.getAllValues().get(1);
        assertEquals(SensTransaction.CREDIT, ligneCredit.getSens());
        assertEquals("Dupont", ligneCredit.getBeneficiaire());
        assertEquals(compteDestinataire, ligneCredit.getCompte());
    }

    // ------------------------------------------------------------------
    // effectuerVirement : cas d'erreur
    // ------------------------------------------------------------------

    @Test
    void effectuerVirement_montantNullNulOuNegatif_estRejeteEn400() {
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, null, "x"));
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, BigDecimal.ZERO, "x"));
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, new BigDecimal("-10"), "x"));

        verifyNoInteractions(compteRepository, beneficiaireRepository, virementRepository);
    }

    @Test
    void effectuerVirement_compteSourceIntrouvable_leveResourceNotFound() {
        when(compteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verifyNoInteractions(virementRepository);
    }

    @Test
    void effectuerVirement_compteDunAutreClient_refuseEn403() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));

        assertReponse(HttpStatus.FORBIDDEN,
                () -> service.effectuerVirement("autre@example.com", 1L, 5L, montant, "x"));

        verify(compteRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void effectuerVirement_compteSourceBloque_estRejeteEn400() {
        compteSource.setStatutCompte(StatutCompte.BLOQUE);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(compteRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void effectuerVirement_beneficiaireIntrouvable_leveResourceNotFound() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));
    }

    @Test
    void effectuerVirement_beneficiaireDunAutreClient_refuseEn403() {
        beneficiaire.setClient(clientDestinataire);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));

        assertReponse(HttpStatus.FORBIDDEN,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));
    }

    @Test
    void effectuerVirement_compteDestinataireIntrouvable_leveResourceNotFound() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));
    }

    @Test
    void effectuerVirement_memeCompteSourceEtDestinataire_estRejeteEn400() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteSource));

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(compteRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void effectuerVirement_devisesDifferentes_estRejeteEn400() {
        compteDestinataire.setDevise("EUR");
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteDestinataire));

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(compteRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void effectuerVirement_compteDestinataireBloque_estRejeteEn400() {
        compteDestinataire.setStatutCompte(StatutCompte.BLOQUE);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteDestinataire));

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(compteRepository, never()).decrementerSoldeSiSoldeSuffisant(any(), any(), any());
    }

    @Test
    void effectuerVirement_soldeInsuffisant_leveInsufficientFunds() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteDestinataire));
        when(compteRepository.decrementerSoldeSiSoldeSuffisant(1L, montant, StatutCompte.ACTIF))
                .thenReturn(0);

        assertThrows(InsufficientFundsException.class,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(compteRepository, never()).incrementerSolde(any(), any(), any());
        verify(virementRepository, never()).save(any());
    }

    @Test
    void effectuerVirement_creditImpossible_estRejeteEn400() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(beneficiaireRepository.findById(5L)).thenReturn(Optional.of(beneficiaire));
        when(compteRepository.findByNumeroCompteOrRib(NUMERO_DESTINATAIRE, NUMERO_DESTINATAIRE))
                .thenReturn(Optional.of(compteDestinataire));
        when(compteRepository.decrementerSoldeSiSoldeSuffisant(1L, montant, StatutCompte.ACTIF))
                .thenReturn(1);
        when(compteRepository.incrementerSolde(2L, montant, StatutCompte.ACTIF))
                .thenReturn(0);

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirement(EMAIL_SOURCE, 1L, 5L, montant, "x"));

        verify(virementRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // effectuerVirementVersCompte
    // ------------------------------------------------------------------

    @Test
    void effectuerVirementVersCompte_succes_normaliseLIdentifiantEtUtiliseUnMotifParDefaut() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(compteRepository.findByNumeroCompteOrRib("MA6400000002", "MA6400000002"))
                .thenReturn(Optional.of(compteDestinataire));
        stubDebitEtCreditReussis();
        stubSave();

        Virement resultat = service.effectuerVirementVersCompte(
                EMAIL_SOURCE, 1L, " ma64-0000 0002 ", montant, null);

        assertEquals("Sara", resultat.getBeneficiaire());
        assertEquals("Virement", resultat.getMotif());
        assertEquals(StatutTransaction.EXECUTEE, resultat.getStatutTransaction());
        verify(compteRepository).findByNumeroCompteOrRib("MA6400000002", "MA6400000002");
    }

    @Test
    void effectuerVirementVersCompte_clientDestinataireSansNom_utiliseBeneficiaireParDefaut() {
        clientDestinataire.setNom("  ");
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(compteRepository.findByNumeroCompteOrRib("MA6400000002", "MA6400000002"))
                .thenReturn(Optional.of(compteDestinataire));
        stubDebitEtCreditReussis();
        stubSave();

        Virement resultat = service.effectuerVirementVersCompte(
                EMAIL_SOURCE, 1L, "MA6400000002", montant, "Cadeau");

        assertEquals("Beneficiaire", resultat.getBeneficiaire());
    }

    @Test
    void effectuerVirementVersCompte_identifiantVideOuNull_estRejeteEn400() {
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirementVersCompte(EMAIL_SOURCE, 1L, "   ", montant, "x"));
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirementVersCompte(EMAIL_SOURCE, 1L, null, montant, "x"));

        verifyNoInteractions(compteRepository, virementRepository);
    }

    @Test
    void effectuerVirementVersCompte_montantInvalide_estRejeteEn400() {
        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirementVersCompte(
                        EMAIL_SOURCE, 1L, "MA6400000002", BigDecimal.ZERO, "x"));

        verifyNoInteractions(compteRepository, virementRepository);
    }

    @Test
    void effectuerVirementVersCompte_compteDestinataireIntrouvable_leveResourceNotFound() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(compteRepository.findByNumeroCompteOrRib("MA6400000002", "MA6400000002"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.effectuerVirementVersCompte(
                        EMAIL_SOURCE, 1L, "MA6400000002", montant, "x"));
    }

    @Test
    void effectuerVirementVersCompte_compteDunAutreClient_refuseEn403() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));

        assertReponse(HttpStatus.FORBIDDEN,
                () -> service.effectuerVirementVersCompte(
                        "autre@example.com", 1L, "MA6400000002", montant, "x"));
    }

    @Test
    void effectuerVirementVersCompte_motifTropLong_estRejeteEn400() {
        String motifTropLong = "a".repeat(256);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteSource));
        when(compteRepository.findByNumeroCompteOrRib("MA6400000002", "MA6400000002"))
                .thenReturn(Optional.of(compteDestinataire));
        stubDebitEtCreditReussis();

        assertReponse(HttpStatus.BAD_REQUEST,
                () -> service.effectuerVirementVersCompte(
                        EMAIL_SOURCE, 1L, "MA6400000002", montant, motifTropLong));

        verify(virementRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // consulterRecu
    // ------------------------------------------------------------------

    @Test
    void consulterRecu_proprietaire_retourneLeVirement() {
        Virement virement = new Virement();
        virement.setCompte(compteSource);
        virement.setCompteDestinataire(compteDestinataire);
        when(virementRepository.findByReference("VIR-20261125-000001"))
                .thenReturn(Optional.of(virement));

        Virement resultat = service.consulterRecu(EMAIL_SOURCE, " vir-20261125-000001 ");

        assertSame(virement, resultat);
    }

    @Test
    void consulterRecu_referenceInconnue_leveResourceNotFound() {
        when(virementRepository.findByReference("VIR-INCONNU"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.consulterRecu(EMAIL_SOURCE, "vir-inconnu"));
    }

    @Test
    void consulterRecu_autreClient_refuseEn403() {
        Virement virement = new Virement();
        virement.setCompte(compteSource);
        virement.setCompteDestinataire(compteDestinataire);
        when(virementRepository.findByReference("VIR-20261125-000001"))
                .thenReturn(Optional.of(virement));

        assertReponse(HttpStatus.FORBIDDEN,
                () -> service.consulterRecu("autre@example.com", "VIR-20261125-000001"));
    }

    @Test
    void consulterRecu_sansCompteDestinataire_leveResourceNotFound() {
        Virement ligneCredit = new Virement();
        ligneCredit.setCompte(compteSource);
        when(virementRepository.findByReference("VIR-20261125-000002"))
                .thenReturn(Optional.of(ligneCredit));

        assertThrows(ResourceNotFoundException.class,
                () -> service.consulterRecu(EMAIL_SOURCE, "VIR-20261125-000002"));
    }

    // ------------------------------------------------------------------
    // consulterHistorique
    // ------------------------------------------------------------------

    @Test
    void consulterHistorique_sansDates_utiliseLaPlageParDefaut() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Virement> page = new PageImpl<>(List.of(new Virement()));
        when(virementRepository.rechercherHistoriqueDuClient(
                any(), any(), any(), any(), any(), any())).thenReturn(page);

        Page<Virement> resultat = service.consulterHistorique(
                EMAIL_SOURCE, null, null, null, pageable);

        assertSame(page, resultat);

        ArgumentCaptor<LocalDateTime> debut = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(virementRepository).rechercherHistoriqueDuClient(
                org.mockito.ArgumentMatchers.eq(EMAIL_SOURCE),
                org.mockito.ArgumentMatchers.eq(SensTransaction.DEBIT),
                debut.capture(), fin.capture(), any(), org.mockito.ArgumentMatchers.eq(pageable));

        assertEquals(LocalDate.of(1970, 1, 1).atStartOfDay(), debut.getValue());
        assertEquals(LocalDate.now().plusDays(1).atStartOfDay(), fin.getValue());
    }

    @Test
    void consulterHistorique_avecDatesEtStatut_transmetLesFiltres() {
        Pageable pageable = PageRequest.of(0, 10);
        when(virementRepository.rechercherHistoriqueDuClient(
                any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.consulterHistorique(
                EMAIL_SOURCE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                StatutTransaction.EXECUTEE,
                pageable);

        verify(virementRepository).rechercherHistoriqueDuClient(
                EMAIL_SOURCE,
                SensTransaction.DEBIT,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0),
                StatutTransaction.EXECUTEE,
                pageable);
    }
}
