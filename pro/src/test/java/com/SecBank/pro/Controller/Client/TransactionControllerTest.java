package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.TransactionController;
import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Transaction;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Services.Clients.TransactionService;
import com.SecBank.pro.Services.CsvExportService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CsvExportService csvExportService;

    @Test
    void transactionsDuCompte_pageParDefautTrieesParDateDesc() throws Exception {
        when(transactionService.rechercherTransactionsDuCompte(
                eq(10L), eq("jean@example.com"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageDeTransactions());

        mockMvc.perform(get("/api/comptes/10/transactions")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu.length()").value(1))
                .andExpect(jsonPath("$.contenu[0].id").value(5))
                .andExpect(jsonPath("$.contenu[0].montant").value(120.00))
                .andExpect(jsonPath("$.contenu[0].sens").value("DEBIT"))
                .andExpect(jsonPath("$.contenu[0].libelle").value("Loyer"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionService).rechercherTransactionsDuCompte(
                eq(10L), eq("jean@example.com"), isNull(), isNull(), isNull(), pageable.capture());

        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
        Sort.Order tri = pageable.getValue().getSort().getOrderFor("dateTransaction");
        assertNotNull(tri);
        assertEquals(Sort.Direction.DESC, tri.getDirection());
    }

    @Test
    void transactionsDuCompte_compteTiers_repond403() throws Exception {
        when(transactionService.rechercherTransactionsDuCompte(
                eq(10L), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new AccesRefuseException("Ce compte n'appartient pas au client connecte"));

        mockMvc.perform(get("/api/comptes/10/transactions")
                        .principal(principal("autre@example.com", "ROLE_CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mesTransactions_filtreParCompteEtSens() throws Exception {
        when(transactionService.rechercherTransactionsDuClient(
                eq("jean@example.com"), eq(10L), isNull(), isNull(), eq(SensTransaction.DEBIT), any(Pageable.class)))
                .thenReturn(pageDeTransactions());

        mockMvc.perform(get("/api/client/transactions")
                        .param("compteId", "10")
                        .param("sens", "DEBIT")
                        .principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].compteId").value(10))
                .andExpect(jsonPath("$.contenu[0].numeroCompte").value("0000000000010"));

        verify(transactionService).rechercherTransactionsDuClient(
                eq("jean@example.com"), eq(10L), isNull(), isNull(), eq(SensTransaction.DEBIT), any(Pageable.class));
    }

    @Test
    void supervisionAdmin_exposeEmetteurEtDestinataire() throws Exception {
        when(transactionService.rechercherToutesLesTransactions(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageDeTransactions());

        mockMvc.perform(get("/api/admin/transactions")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].emetteur").value("Jean Dupont"))
                .andExpect(jsonPath("$.contenu[0].destinataire").value("Proprietaire"))
                .andExpect(jsonPath("$.contenu[0].devise").value("MAD"));
    }

    @Test
    void supervisionAdmin_listeToutesLesTransactions() throws Exception {
        when(transactionService.rechercherToutesLesTransactions(
                isNull(), isNull(), any(LocalDate.class), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageDeTransactions());

        mockMvc.perform(get("/api/admin/transactions")
                        .param("du", "2026-01-01")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].id").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void exportCsv_retourneUnFichierTelechargeableAvecLesFiltres() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/export-csv")
                        .param("du", "2026-01-01")
                        .param("sens", "DEBIT")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void exportCsv_leCorpsEstUnFluxCsvAvecToutesLesColonnes() throws Exception {
        doAnswer(invocation -> {
            OutputStream flux = invocation.getArgument(0);
            flux.write(("\uFEFFDate;Emetteur;Destinataire;Montant;Devise;Statut;Sens;Compte;Libelle\r\n"
                    + "05/01/2026 10:00:00;Jean Dupont;Proprietaire;120.00;MAD;EXECUTEE;DEBIT;"
                    + "0000000000010;Loyer\r\n").getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(csvExportService).exporter(any(), any(), any(), any(), any(), any());

        MvcResult resultat = mockMvc.perform(get("/api/admin/transactions/export-csv")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(resultat))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Date;Emetteur;Destinataire;Montant;Devise;Statut;Sens;Compte;Libelle")))
                .andExpect(content().string(containsString(
                        "05/01/2026 10:00:00;Jean Dupont;Proprietaire;120.00;MAD;EXECUTEE;DEBIT;")));
    }

    @Test
    void exportCsv_periodeIncoherente_repond400() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/export-csv")
                        .param("du", "2026-02-01")
                        .param("au", "2026-01-01")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tailleHorsBornes_repond400() throws Exception {
        mockMvc.perform(get("/api/comptes/10/transactions")
                        .param("size", "0")
                        .principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodeIncoherente_repond400() throws Exception {
        mockMvc.perform(get("/api/comptes/10/transactions")
                        .param("du", "2026-02-01")
                        .param("au", "2026-01-01")
                        .principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());
    }

    private static Principal principal(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private static Page<Transaction> pageDeTransactions() {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");
        client.setNom("Jean Dupont");

        CompteBancaire compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setDevise("MAD");
        compte.setClient(client);

        Virement virement = new Virement();
        virement.setId(5L);
        virement.setMontant(new BigDecimal("120.00"));
        virement.setSens(SensTransaction.DEBIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setDateTransaction(LocalDateTime.of(2026, 1, 5, 10, 0));
        virement.setMotif("Loyer");
        virement.setBeneficiaire("Proprietaire");
        virement.setCompte(compte);

        return new PageImpl<>(List.of(virement), PageRequest.of(0, 10), 1);
    }
}
