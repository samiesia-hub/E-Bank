package com.SecBank.pro.Controller.Client;

import com.SecBank.pro.Conrollers.Clients.ClientProfileController;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Models.Cliens.Client;
import com.SecBank.pro.Services.Clients.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientProfileController.class)
class ClientProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void monProfil_neRenvoiePasLeMotDePasse() throws Exception {
        Client client = new Client();
        client.setId_Utilisateur(1L);
        client.setEmail("jean@example.com");
        client.setNom("Jean");
        client.setPassword("hash-secret");
        when(clientService.consulterProfil("jean@example.com")).thenReturn(client);

        mockMvc.perform(get("/api/client/profile").principal(principal("jean@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@example.com"))
                .andExpect(jsonPath("$.nom").value("Jean"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void changerMotDePasse_ancienFaux_retourne400() throws Exception {
        doThrow(new DonneeInvalideException("Mot de passe actuel incorrect"))
                .when(clientService).changerMotDePasse(eq("jean@example.com"), any());

        mockMvc.perform(post("/api/client/change-password")
                        .principal(principal("jean@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasseActuel\":\"faux\",\"nouveauMotDePasse\":\"NouveauMotDePasse2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mot de passe actuel incorrect"));
    }

    @Test
    void changerMotDePasse_ancienCorrect_retourne200() throws Exception {
        mockMvc.perform(post("/api/client/change-password")
                        .principal(principal("jean@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasseActuel\":\"AncienMotDePasse1\",\"nouveauMotDePasse\":\"NouveauMotDePasse2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mot de passe modifié"));
    }

    private static Principal principal(String email) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }
}
