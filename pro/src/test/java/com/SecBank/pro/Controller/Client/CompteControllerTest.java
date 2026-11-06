package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.CompteController;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Models.Cliens.CompteBancaire;
import com.SecBank.pro.Models.Enums.StatutCompte;
import com.SecBank.pro.Models.Enums.TypeCompte;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(compteService.creerCompte(1L, TypeCompte.COURANT)).thenReturn(compte());

        mockMvc.perform(post("/api/admin/comptes")
                        .principal(principal("admin@example.com", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"typeCompte\":\"COURANT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rib").value("011780001000000000001045"))
                .andExpect(jsonPath("$.devise").value("MAD"))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"));
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
