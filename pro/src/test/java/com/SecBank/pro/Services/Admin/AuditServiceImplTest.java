package com.SecBank.pro.Services.Admin;

import com.SecBank.pro.Models.Admin.JournalAudit;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.Admin.JournalAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private JournalAuditRepository journalAuditRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    void journaliser_conserveAgentMotifEtHorodatage() {

        // Arrange
        Utilisateur agent = new Utilisateur();
        agent.setId_Utilisateur(5L);
        agent.setEmail("admin@example.com");

        when(journalAuditRepository.save(any(JournalAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        JournalAudit entree = auditService.journaliser(
                "DEPOT_GUICHET", agent, "Depot de 500.00 MAD sur le compte 0000000000010 (id 10)");

        // Assert
        assertEquals("DEPOT_GUICHET", entree.getAction());
        assertEquals(agent, entree.getUtilisateur());
        assertNotNull(entree.getDateAction(), "l'entree d'audit doit etre horodatee");
        verify(journalAuditRepository, times(1)).save(entree);
    }

    @Test
    void consulterJournauxAudit_lesPlusRecentsDAbord() {

        // Arrange
        when(journalAuditRepository.findAll(any(Sort.class))).thenReturn(List.of());

        // Act
        auditService.consulterJournauxAudit();

        // Assert
        ArgumentCaptor<Sort> tri = ArgumentCaptor.forClass(Sort.class);
        verify(journalAuditRepository).findAll(tri.capture());
        assertEquals(Sort.Direction.DESC, tri.getValue().getOrderFor("dateAction").getDirection());
    }
}
