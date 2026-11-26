package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.VirementController;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Cliens.Virement;
import com.SecBank.pro.Models.Enums.SensTransaction;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.StatutTransaction;
import com.SecBank.pro.Services.Clients.VirementService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VirementController.class)
class VirementControllerTest {

    private static final String EMAIL = "jean@example.com";
    private static final String REFERENCE = "VIR-20261125-000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VirementService virementService;

    // ------------------------------------------------------------------
    // POST /api/virements
    // ------------------------------------------------------------------

    @Test
    void effectuerVirement_versBeneficiaire_retourne201EtAppelleLeBonService() throws Exception {
        when(virementService.effectuerVirement(
                EMAIL, 1L, 5L, new BigDecimal("150"), "Loyer"))
                .thenReturn(virementComplet());

        mockMvc.perform(post("/api/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteSourceId":1,"beneficiaireId":5,"montant":150,"motif":"Loyer"}
                                """))
                .andExpect(status().isCreated());

        verify(virementService).effectuerVirement(
                EMAIL, 1L, 5L, new BigDecimal("150"), "Loyer");
        verify(virementService, never()).effectuerVirementVersCompte(
                any(), any(), any(), any(), any());
    }

    @Test
    void effectuerVirement_versUnRib_retourne201EtAppelleLeServiceVersCompte() throws Exception {
        when(virementService.effectuerVirementVersCompte(
                EMAIL, 1L, "011780001000000000001045", new BigDecimal("80"), "Cadeau"))
                .thenReturn(virementComplet());

        mockMvc.perform(post("/api/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteSourceId":1,"numeroCompteBeneficiaire":"011780001000000000001045","montant":80,"motif":"Cadeau"}
                                """))
                .andExpect(status().isCreated());

        verify(virementService).effectuerVirementVersCompte(
                EMAIL, 1L, "011780001000000000001045", new BigDecimal("80"), "Cadeau");
        verify(virementService, never()).effectuerVirement(
                any(), any(), any(), any(), any());
    }

    @Test
    void effectuerVirement_sansCompteSource_retourne400() throws Exception {
        mockMvc.perform(post("/api/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"beneficiaireId":5,"montant":150}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(virementService);
    }

    @Test
    void effectuerVirement_sansBeneficiaireNiRib_retourne400() throws Exception {
        mockMvc.perform(post("/api/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteSourceId":1,"numeroCompteBeneficiaire":"   ","montant":150}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(virementService);
    }

    @Test
    void effectuerVirement_parUnAdmin_retourne403() throws Exception {
        mockMvc.perform(post("/api/virements")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteSourceId":1,"beneficiaireId":5,"montant":150}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(virementService);
    }

    // ------------------------------------------------------------------
    // GET /api/client/virements (historique)
    // ------------------------------------------------------------------

    @Test
    void historique_parDefaut_utilisePage0Taille10EtTriParDateDecroissante() throws Exception {
        when(virementService.consulterHistorique(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/client/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(virementService).consulterHistorique(
                eq(EMAIL), any(), any(), any(), pageable.capture());

        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());

        Sort.Order parDate = pageable.getValue().getSort().getOrderFor("dateTransaction");
        assertNotNull(parDate);
        assertEquals(Sort.Direction.DESC, parDate.getDirection());
        assertNotNull(pageable.getValue().getSort().getOrderFor("id"));
    }

    @Test
    void historique_avecFiltres_lesTransmetAuService() throws Exception {
        when(virementService.consulterHistorique(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/client/virements")
                        .param("page", "1")
                        .param("size", "5")
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-01-31")
                        .param("statut", "EXECUTEE")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(virementService).consulterHistorique(
                eq(EMAIL),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 1, 31)),
                eq(StatutTransaction.EXECUTEE),
                pageable.capture());

        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
    }

    @Test
    void historique_avecUnVirement_retourne200() throws Exception {
        when(virementService.consulterHistorique(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(virementComplet())));

        mockMvc.perform(get("/api/client/virements")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isOk());
    }

    @Test
    void historique_pageNegative_retourne400() throws Exception {
        mockMvc.perform(get("/api/client/virements")
                        .param("page", "-1")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(virementService);
    }

    @Test
    void historique_tailleHorsLimites_retourne400() throws Exception {
        mockMvc.perform(get("/api/client/virements")
                        .param("size", "0")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/client/virements")
                        .param("size", "101")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(virementService);
    }

    @Test
    void historique_periodeInversee_retourne400() throws Exception {
        mockMvc.perform(get("/api/client/virements")
                        .param("dateDebut", "2026-02-01")
                        .param("dateFin", "2026-01-01")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(virementService);
    }

    @Test
    void historique_parUnAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/client/virements")
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(virementService);
    }

    // ------------------------------------------------------------------
    // GET /api/client/virements/{reference} (recu)
    // ------------------------------------------------------------------

    @Test
    void consulterRecu_retourne200EtTransmetLaReference() throws Exception {
        when(virementService.consulterRecu(EMAIL, REFERENCE))
                .thenReturn(virementComplet());

        mockMvc.perform(get("/api/client/virements/" + REFERENCE)
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isOk());

        verify(virementService).consulterRecu(EMAIL, REFERENCE);
    }

    @Test
    void consulterRecu_referenceInconnue_retourne404() throws Exception {
        when(virementService.consulterRecu(EMAIL, "VIR-INCONNU"))
                .thenThrow(new ResourceNotFoundException("Virement introuvable"));

        mockMvc.perform(get("/api/client/virements/VIR-INCONNU")
                        .principal(principal(EMAIL, "ROLE_CLIENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void consulterRecu_parUnAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/client/virements/" + REFERENCE)
                        .principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(virementService);
    }

    // ------------------------------------------------------------------
    // Donnees de test
    // ------------------------------------------------------------------

    private static Principal principal(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
    }

    /** Virement entierement renseigne, pour que les DTO de reponse puissent le convertir. */
    private static Virement virementComplet() {
        Client clientSource = new Client();
        clientSource.setId_Utilisateur(1L);
        clientSource.setEmail(EMAIL);
        clientSource.setNom("Dupont");

        Client clientDestinataire = new Client();
        clientDestinataire.setId_Utilisateur(2L);
        clientDestinataire.setEmail("sara@example.com");
        clientDestinataire.setNom("Sara");

        CompteBancaire source = new CompteBancaire();
        source.setId(1L);
        source.setNumeroCompte("0000000000001");
        source.setRib("011780001000000000001045");
        source.setDevise("MAD");
        source.setSolde(new BigDecimal("1000.00"));
        source.setStatutCompte(StatutCompte.ACTIF);
        source.setClient(clientSource);

        CompteBancaire destination = new CompteBancaire();
        destination.setId(2L);
        destination.setNumeroCompte("0000000000002");
        destination.setRib("011780001000000000002046");
        destination.setDevise("MAD");
        destination.setSolde(new BigDecimal("500.00"));
        destination.setStatutCompte(StatutCompte.ACTIF);
        destination.setClient(clientDestinataire);

        Virement virement = new Virement();
        virement.setId(100L);
        virement.setReference(REFERENCE);
        virement.setCompte(source);
        virement.setCompteDestinataire(destination);
        virement.setMontant(new BigDecimal("150.00"));
        virement.setSens(SensTransaction.DEBIT);
        virement.setStatutTransaction(StatutTransaction.EXECUTEE);
        virement.setDateTransaction(LocalDateTime.of(2026, 11, 25, 10, 0));
        virement.setBeneficiaire("Sara Alami");
        virement.setMotif("Loyer");
        return virement;
    }
}
