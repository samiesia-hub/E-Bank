package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.CompteController;
import com.SecBank.pro.Exceptions.RibDejaUtiliseException;
import com.SecBank.pro.Exceptions.RibInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
import com.SecBank.pro.Services.Clients.CompteService;
import com.SecBank.pro.Utils.IbanGenerator;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompteController.class)
class CompteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompteService compteService;

    @Test
    void mesComptes_listeLesComptesDuClientConnecte() throws Exception {
        when(compteService.consulterComptesDuClient("jean@example.com"))
                .thenReturn(List.of(compte()));

        mockMvc.perform(get("/api/client/comptes").principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].numeroCompte").value("0000000000010"))
                .andExpect(jsonPath("$[0].devise").value("MAD"))
                .andExpect(jsonPath("$[0].solde").value(1500.00));
    }

    @Test
    void detailCompte_retourneLeSoldeDuCompteDuClient() throws Exception {
        when(compteService.consulterCompteDuClient(10L, "jean@example.com"))
                .thenReturn(compte());

        mockMvc.perform(get("/api/comptes/10").principal(principal("jean@example.com", "ROLE_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.rib").value("011780001000000000001045"))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"));
    }

    @Test
    void creationCompte_adminCreeUnCompteActifEnMAD() throws Exception {
        when(compteService.creerCompte(1L, TypeCompte.COURANT, null, null)).thenReturn(compte());

        mockMvc.perform(post("/api/admin/comptes")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rib").value("011780001000000000001045"))
                .andExpect(jsonPath("$.devise").value("MAD"))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"));
    }

    @Test
    void creationCompte_ribInvalide_retourne400AvecLeMotifExact() throws Exception {
        String rib = "011780001000000000001046";
        when(compteService.creerCompte(eq(1L), any(TypeCompte.class), eq(rib), any()))
                .thenThrow(new RibInvalideException(IbanGenerator.MOTIF_RIB_CLE));

        mockMvc.perform(post("/api/admin/comptes")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\",\"rib\":\"" + rib + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(IbanGenerator.MOTIF_RIB_CLE));
    }

    @Test
    void creationCompte_ribDejaUtilise_retourne409() throws Exception {
        String rib = "011780001000000000001045";
        when(compteService.creerCompte(eq(1L), any(TypeCompte.class), eq(rib), any()))
                .thenThrow(new RibDejaUtiliseException("Un compte avec le RIB " + rib + " existe deja"));

        mockMvc.perform(post("/api/admin/comptes")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\",\"rib\":\"" + rib + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un compte avec le RIB " + rib + " existe deja"));
    }

    @Test
    void ouvertureAvecSoldeInitial500_stockeLeSoldeEtLeRib() throws Exception {
        CompteBancaire compte = compte();
        compte.setSolde(new BigDecimal("500.00"));
        when(compteService.creerCompte(eq(1L), eq(TypeCompte.COURANT), eq(null), eq(new BigDecimal("500"))))
                .thenReturn(compte);

        mockMvc.perform(post("/api/admin/comptes")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\",\"soldeInitial\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solde").value(500.00))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"))
                .andExpect(jsonPath("$.rib").isNotEmpty());
    }

    @Test
    void listeAdmin_retourneTousLesComptesAvecLeurTitulaire() throws Exception {
        when(compteService.listerTousLesComptes()).thenReturn(List.of(compte()));

        mockMvc.perform(get("/api/admin/comptes").principal(principal("admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroCompte").value("0000000000010"))
                .andExpect(jsonPath("$[0].clientEmail").value("jean@example.com"))
                .andExpect(jsonPath("$[0].statutCompte").value("ACTIF"));
    }

    @Test
    void gelerCompte_retourneLeCompteBloque() throws Exception {
        CompteBancaire compte = compte();
        compte.setStatutCompte(StatutCompte.BLOQUE);
        when(compteService.changerStatut(10L, StatutCompte.BLOQUE)).thenReturn(compte);

        mockMvc.perform(patch("/api/admin/comptes/10/statut")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"BLOQUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutCompte").value("BLOQUE"));
    }

    private static Principal principal(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private static CompteBancaire compte() {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");

        CompteBancaire compte = new CompteBancaire();
        compte.setId(10L);
        compte.setNumeroCompte("0000000000010");
        compte.setRib("011780001000000000001045");
        compte.setDevise("MAD");
        compte.setSolde(new BigDecimal("1500.00"));
        compte.setTypeCompte(TypeCompte.COURANT);
        compte.setStatutCompte(StatutCompte.ACTIF);
        compte.setClient(client);
        return compte;
    }
}
