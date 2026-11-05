package com.SecBank.pro;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Configurations.SecurityConfig;
import com.SecBank.pro.Conrollers.AuthController;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void inscriptionRetourneUnToken() throws Exception {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("sami@example.com");
        given(authService.inscrire("sami@example.com", "secret123", "Sami")).willReturn(utilisateur);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sami@example.com","motDePasse":"secret123","nom":"Sami"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void inscriptionRefuseeSiEmailDejaUtilise() throws Exception {
        given(authService.inscrire("sami@example.com", "secret123", "Sami"))
                .willThrow(new IllegalArgumentException("Cet email est déjà utilisé"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sami@example.com","motDePasse":"secret123","nom":"Sami"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur").value("Cet email est déjà utilisé"));
    }

    @Test
    void connexionReussieRetourneUnToken() throws Exception {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("sami@example.com");
        utilisateur.setNom("Sami");
        given(authService.seConnecter("sami@example.com", "secret123")).willReturn(true);
        given(authService.trouverParEmail("sami@example.com")).willReturn(java.util.Optional.of(utilisateur));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sami@example.com","motDePasse":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void connexionRefuseeAvecMauvaisMotDePasse() throws Exception {
        given(authService.seConnecter("sami@example.com", "mauvais")).willReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sami@example.com","motDePasse":"mauvais"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routeProtegeeRefuseeSansToken() throws Exception {
        mockMvc.perform(get("/api/comptes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routeProtegeeAccepteeAvecTokenValide() throws Exception {
        String token = jwtService.creerToken("sami@example.com", "CLIENT");

        // Aucun controller ne gère /api/comptes : 404 prouve que la sécurité est passée.
        mockMvc.perform(get("/api/comptes").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void routeProtegeeRefuseeAvecTokenInvalide() throws Exception {
        mockMvc.perform(get("/api/comptes").header("Authorization", "Bearer faux-token"))
                .andExpect(status().isUnauthorized());
    }
}
