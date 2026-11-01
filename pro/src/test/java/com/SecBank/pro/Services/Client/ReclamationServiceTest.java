package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.Reclamation;
import com.SecBank.pro.Models.Énums.StatusReclamation;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.ReclamationRepo;
import com.SecBank.pro.Services.Clients.ReclamationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReclamationServiceTest {

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
    }

    @Test
    void creerReclamation_Success() {

        // Arrange
        Long clientId = 1L;
        String objet = "Problème de virement";
        String description = "Mon virement n'a pas été effectué.";

        when(clientRepo.findById(clientId))
                .thenReturn(Optional.of(client));

        when(reclamationRepo.save(any(Reclamation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reclamation result = reclamationService.creerReclamation(
                clientId, objet, description
        );

        // Assert
        assertNotNull(result);
        assertEquals(objet, result.getObjet());
        assertEquals(description, result.getDesciprtion());
        assertEquals(StatusReclamation.OUVERTE, result.getStatus());
        assertEquals(client, result.getClient());
        assertNotNull(result.getDateCreation());

        verify(clientRepo, times(1)).findById(clientId);
        verify(reclamationRepo, times(1)).save(any(Reclamation.class));
    }

    @Test
    void creerReclamation_ClientNotFound() {

        // Arrange
        Long clientId = 99L;

        when(clientRepo.findById(clientId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () ->
                reclamationService.creerReclamation(
                        clientId,
                        "Problème",
                        "Description"
                )
        );

        verify(reclamationRepo, never()).save(any(Reclamation.class));
    }

    @Test
    void suivreReclamation_Success() {

        // Arrange
        Long reclamationId = 1L;
        Reclamation reclamation = new Reclamation();

        when(reclamationRepo.findById(reclamationId))
                .thenReturn(Optional.of(reclamation));

        // Act
        Reclamation result =
                reclamationService.suivreReclamation(reclamationId);

        // Assert
        assertNotNull(result);
        assertSame(reclamation, result);

        verify(reclamationRepo, times(1)).findById(reclamationId);
    }

    @Test
    void suivreReclamation_NotFound() {

        // Arrange
        Long reclamationId = 99L;

        when(reclamationRepo.findById(reclamationId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () ->
                reclamationService.suivreReclamation(reclamationId)
        );

        verify(reclamationRepo, times(1)).findById(reclamationId);
    }
}