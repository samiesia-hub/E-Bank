package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Services.Clients.CompteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {

    @Mock
    private ClientRepo clientRepo;

    @Mock
    private CompteBancaireRepository compteBancaireRepository;

    @InjectMocks
    private CompteServiceImpl compteService;

    private Client client;

    private CompteBancaire compte;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId_Utilisateur(1L);

        compte = new CompteBancaire();
        compte.setId(10L);
        compte.setSolde(new BigDecimal("1500.00"));
        compte.setClient(client);
    }

    @Test
    void consulterSolde_Success() {

        // Arrange
        Long compteId = 10L;

        when(compteBancaireRepository.findById(compteId))
                .thenReturn(Optional.of(compte));

        // Act
        BigDecimal solde = compteService.consulterSolde(compteId);

        // Assert
        assertNotNull(solde);
        assertEquals(new BigDecimal("1500.00"), solde);

        verify(compteBancaireRepository, times(1)).findById(compteId);
    }

    @Test
    void consulterSolde_CompteNotFound() {

        // Arrange
        Long compteId = 99L;

        when(compteBancaireRepository.findById(compteId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                compteService.consulterSolde(compteId)
        );

        verify(compteBancaireRepository, times(1)).findById(compteId);
    }

    @Test
    void consulterComptes_Success() {

        // Arrange
        Long clientId = 1L;

        when(clientRepo.findById(clientId))
                .thenReturn(Optional.of(client));
        when(compteBancaireRepository.findByClientId(clientId))
                .thenReturn(List.of(compte));

        // Act
        List<CompteBancaire> comptes = compteService.consulterComptes(clientId);

        // Assert
        assertNotNull(comptes);
        assertEquals(1, comptes.size());
        assertEquals(compte, comptes.get(0));

        verify(clientRepo, times(1)).findById(clientId);
        verify(compteBancaireRepository, times(1)).findByClientId(clientId);
    }

    @Test
    void consulterComptes_ClientNotFound() {

        // Arrange
        Long clientId = 99L;

        when(clientRepo.findById(clientId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                compteService.consulterComptes(clientId)
        );

        verify(compteBancaireRepository, never()).findByClientId(clientId);
    }

    @Test
    void creerCompte_genereNumeroRibDeviseEtStatutActif() {

        // Arrange
        when(clientRepo.findById(1L))
                .thenReturn(Optional.of(client));
        when(compteBancaireRepository.save(any(CompteBancaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CompteBancaire cree = compteService.creerCompte(1L, TypeCompte.EPARGNE);

        // Assert : numero de compte et RIB uniques, coherents et en MAD
        assertNotNull(cree.getNumeroCompte());
        assertEquals(13, cree.getNumeroCompte().length());
        assertNotNull(cree.getRib());
        assertTrue(cree.getRib().matches("\\d{24}"), "RIB attendu sur 24 chiffres");
        assertEquals(cree.getNumeroCompte(), cree.getRib().substring(9, 22));
        assertEquals(0, new BigInteger(cree.getRib()).mod(BigInteger.valueOf(97)).intValue(),
                "la cle du RIB doit rendre le RIB divisible par 97");

        assertEquals("MAD", cree.getDevise());
        assertEquals(new BigDecimal("0.00"), cree.getSolde());
        assertEquals(TypeCompte.EPARGNE, cree.getTypeCompte());
        assertEquals(StatutCompte.ACTIF, cree.getStatutCompte());
        assertNotNull(cree.getDateCreation());
        assertEquals(client, cree.getClient());

        verify(compteBancaireRepository, times(1)).save(cree);
    }

    @Test
    void creerCompte_typeCourantParDefaut() {

        // Arrange
        when(clientRepo.findById(1L))
                .thenReturn(Optional.of(client));
        when(compteBancaireRepository.save(any(CompteBancaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CompteBancaire cree = compteService.creerCompte(1L, null);

        // Assert
        assertEquals(TypeCompte.COURANT, cree.getTypeCompte());
        assertEquals(StatutCompte.ACTIF, cree.getStatutCompte());
    }

    @Test
    void creerCompte_clientNotFound() {

        // Arrange
        when(clientRepo.findById(99L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                compteService.creerCompte(99L, TypeCompte.COURANT)
        );

        verify(compteBancaireRepository, never()).save(any(CompteBancaire.class));
    }

    @Test
    void consulterComptesDuClient_filtreParEmailDuToken() {

        // Arrange
        client.setEmail("jean@example.com");
        when(clientRepo.findByEmail("jean@example.com"))
                .thenReturn(Optional.of(client));
        when(compteBancaireRepository.findByClientId(1L))
                .thenReturn(List.of(compte));

        // Act
        List<CompteBancaire> comptes = compteService.consulterComptesDuClient("jean@example.com");

        // Assert
        assertEquals(1, comptes.size());
        assertEquals(compte, comptes.get(0));
        verify(compteBancaireRepository, times(1)).findByClientId(1L);
    }

    @Test
    void consulterCompteDuClient_proprietaire() {

        // Arrange
        client.setEmail("jean@example.com");
        when(compteBancaireRepository.findById(10L))
                .thenReturn(Optional.of(compte));

        // Act
        CompteBancaire trouve = compteService.consulterCompteDuClient(10L, "jean@example.com");

        // Assert
        assertEquals(compte, trouve);
        assertEquals(new BigDecimal("1500.00"), trouve.getSolde());
    }

    @Test
    void consulterCompteDuClient_refuseLeCompteDUnAutreClient() {

        // Arrange
        client.setEmail("jean@example.com");
        when(compteBancaireRepository.findById(10L))
                .thenReturn(Optional.of(compte));

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () ->
                compteService.consulterCompteDuClient(10L, "autre@example.com")
        );
    }
}
