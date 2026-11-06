package com.SecBank.pro.Services.Client;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Repositories.Client.ClientRepo;
import com.SecBank.pro.Repositories.Client.CompteBancaireRepository;
import com.SecBank.pro.Repositories.Client.TransactionRepository;
import com.SecBank.pro.Services.Clients.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CompteBancaireRepository compteBancaireRepository;

    @Mock
    private ClientRepo clientRepo;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Client client;

    private CompteBancaire compte;

    private Virement virement;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");

        compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setClient(client);

        virement = new Virement();
        virement.setId(5L);
        virement.setMontant(new BigDecimal("120.00"));
        virement.setSens(SensTransaction.DEBIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setDateTransaction(LocalDateTime.of(2026, 1, 5, 10, 0));
        virement.setMotif("Loyer");
        virement.setCompte(compte);
    }

    @Test
    void transactionsDuCompte_proprietaire_retourneUnePageTrieeParDateDesc() {

        // Arrange
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(virement), PageRequest.of(0, 10), 1));

        // Act
        Page<Transaction> page = transactionService.rechercherTransactionsDuCompte(
                10L, "jean@example.com", null, null, null, PageRequest.of(0, 10));

        // Assert
        assertEquals(1, page.getTotalElements());
        assertEquals(virement, page.getContent().get(0));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository, times(1)).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());

        Sort.Order tri = pageable.getValue().getSort().getOrderFor("dateTransaction");
        assertNotNull(tri, "les transactions doivent etre triees par dateTransaction");
        assertEquals(Sort.Direction.DESC, tri.getDirection());
    }

    @Test
    void transactionsDuCompte_tiers_refuseEn403() {

        // Arrange
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));

        // Act + Assert
        assertThrows(AccesRefuseException.class, () ->
                transactionService.rechercherTransactionsDuCompte(
                        10L, "autre@example.com", null, null, null, PageRequest.of(0, 10))
        );

        verify(transactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void accesRefuse_estBienMappeEn403() {
        ResponseStatus statut = AccesRefuseException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(statut);
        assertEquals(HttpStatus.FORBIDDEN, statut.value());
    }

    @Test
    void transactionsDuClient_sansCompte_filtreSurSesComptes() {

        // Arrange
        when(clientRepo.findByEmail("jean@example.com")).thenReturn(Optional.of(client));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(virement)));

        // Act
        Page<Transaction> page = transactionService.rechercherTransactionsDuClient(
                "jean@example.com", null, null, null, null, PageRequest.of(0, 10));

        // Assert
        assertEquals(1, page.getContent().size());
        verify(clientRepo, times(1)).findByEmail("jean@example.com");
        verify(compteBancaireRepository, never()).findById(any());
    }

    @Test
    void transactionsDuClient_compteTiers_refuseEn403() {

        // Arrange
        when(compteBancaireRepository.findById(10L)).thenReturn(Optional.of(compte));

        // Act + Assert
        assertThrows(AccesRefuseException.class, () ->
                transactionService.rechercherTransactionsDuClient(
                        "autre@example.com", 10L, null, null, null, PageRequest.of(0, 10))
        );
    }

    @Test
    void supervisionAdmin_neVerifiePasLaPropriete() {

        // Arrange
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(virement)));

        // Act
        Page<Transaction> page = transactionService.rechercherToutesLesTransactions(
                null, null, null, null, null, PageRequest.of(0, 10));

        // Assert
        assertEquals(1, page.getContent().size());
        verifyNoInteractions(compteBancaireRepository, clientRepo);
    }

    @Test
    void periodeInvalide_estRejetee() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.rechercherToutesLesTransactions(
                        null, 10L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1),
                        null, PageRequest.of(0, 10))
        );
    }

    @Test
    void consulterHistorique_retourneLesTransactionsDuCompte() {

        // Arrange
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(virement));

        // Act
        List<Transaction> transactions = transactionService.consulterHistorique(10L);

        // Assert
        assertEquals(1, transactions.size());
        assertEquals(virement, transactions.get(0));
    }
}
