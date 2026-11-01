package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
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
}
