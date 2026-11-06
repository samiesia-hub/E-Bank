package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Admin.OperationController;
import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.OperationGuichet;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeOperation;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Services.Clients.CompteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationController.class)
class OperationControllerTest {

    private static final String EMAIL_AGENT = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompteService compteService;

    @Test
    void depot_crediteLeCompteEtRetourneLaTransaction() throws Exception {

        // Arrange
        when(compteService.deposer(eq(10L), any(BigDecimal.class), eq("Versement especes"), eq(EMAIL_AGENT)))
                .thenReturn(operation(TypeOperation.DEPOT, "2000.00", SensTransaction.CREDIT));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/10/depot")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":500,\"motif\":\"Versement especes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeOperation").value("DEPOT"))
                .andExpect(jsonPath("$.montant").value(500.00))
                .andExpect(jsonPath("$.nouveauSolde").value(2000.00))
                .andExpect(jsonPath("$.numeroCompte").value("0000000000010"))
                .andExpect(jsonPath("$.agent").value(EMAIL_AGENT));
    }

    @Test
    void retrait_soldeInsuffisant_retourne400AvecLeMessage() throws Exception {

        // Arrange
        when(compteService.retirer(eq(10L), any(BigDecimal.class), any(), eq(EMAIL_AGENT)))
                .thenThrow(new OperationRefuseeException("Solde insuffisant : solde 200.00, retrait demande 500.00"));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/10/retrait")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":500,\"motif\":\"Retrait especes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solde insuffisant : solde 200.00, retrait demande 500.00"));
    }

    @Test
    void montantNegatif_retourne400AvecLeMessageDeValidation() throws Exception {

        // Arrange
        when(compteService.deposer(eq(10L), any(BigDecimal.class), any(), eq(EMAIL_AGENT)))
                .thenThrow(new OperationRefuseeException("Le montant doit etre strictement positif"));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/10/depot")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":-500,\"motif\":\"Erreur\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le montant doit etre strictement positif"));
    }

    @Test
    void montantAbsent_retourne400AvecLeMessageDeValidation() throws Exception {

        // Arrange
        when(compteService.deposer(eq(10L), any(), any(), eq(EMAIL_AGENT)))
                .thenThrow(new OperationRefuseeException("Le montant est obligatoire"));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/10/depot")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Sans montant\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le montant est obligatoire"));
    }

    @Test
    void compteInconnu_retourne404() throws Exception {

        // Arrange
        when(compteService.deposer(eq(99L), any(BigDecimal.class), any(), eq(EMAIL_AGENT)))
                .thenThrow(new ResourceNotFoundException("Compte introuvable avec id :99"));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/99/depot")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":500,\"motif\":\"Versement\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retrait_soldeSuffisant_retourneLaTransactionRetrait() throws Exception {

        // Arrange
        when(compteService.retirer(eq(10L), any(BigDecimal.class), eq("Retrait especes"), eq(EMAIL_AGENT)))
                .thenReturn(operation(TypeOperation.RETRAIT, "300.00", SensTransaction.DEBIT));

        // Act + Assert
        mockMvc.perform(post("/api/admin/comptes/10/retrait")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":200,\"motif\":\"Retrait especes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeOperation").value("RETRAIT"))
                .andExpect(jsonPath("$.nouveauSolde").value(300.00));
    }

    private static Principal principal() {
        return new UsernamePasswordAuthenticationToken(
                EMAIL_AGENT, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static OperationGuichet operation(TypeOperation type, String solde, SensTransaction sens) {
        CompteBancaire compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setDevise("MAD");
        compte.setSolde(new BigDecimal(solde));

        Utilisateur agent = new Utilisateur();
        agent.setEmail(EMAIL_AGENT);

        OperationGuichet operation = new OperationGuichet();
        operation.setId(99L);
        operation.setTypeOperation(type);
        operation.setSens(sens);
        operation.setStatutTransaction(StatutTransaction.EXECUTEE);
        operation.setMontant(new BigDecimal("500.00"));
        operation.setMotif(type == TypeOperation.DEPOT ? "Versement especes" : "Retrait especes");
        operation.setCompte(compte);
        operation.setAgent(agent);
        operation.setDateTransaction(LocalDateTime.of(2026, 11, 6, 10, 0));
        return operation;
    }
}
