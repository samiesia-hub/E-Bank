package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Dtos.RefreshTokenRequest;
import com.SecBank.pro.Dtos.ResetPasswordDTO;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Services.AuthService;
import com.SecBank.pro.Services.RefreshTokenService;
import com.SecBank.pro.Services.ReinitialisationMotDePasseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification",
        description = "Inscription, connexion, rafraichissement et deconnexion. Routes publiques.")
public class AuthController {

    private static final String COOKIE_SESSION = "session";

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ReinitialisationMotDePasseService reinitialisationService;
    private final boolean cookieSecurise;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          ReinitialisationMotDePasseService reinitialisationService,
                          @Value("${app.cookie.secure:true}") boolean cookieSecurise) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.reinitialisationService = reinitialisationService;
        this.cookieSecurise = cookieSecurise;
    }

    // ------------------------------------------------------------------
    // Inscription
    // ------------------------------------------------------------------
    @Operation(summary = "Inscrire un nouveau client",
            description = "Cree le client puis ouvre une session : jeton JWT dans le corps (`token`), "
                    + "refresh token (`refreshToken`) et cookie httpOnly `session`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte cree, jeton renvoye"),
            @ApiResponse(responseCode = "400", description = "Email deja utilise ou donnees invalides",
                    content = @Content(examples = @ExampleObject(value = "{\"erreur\":\"Email deja utilise\"}")))
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> inscription(@Valid @RequestBody InscriptionRequete requete) {
        try {
            Utilisateur utilisateur = authService.inscrire(
                    requete.email(), requete.motDePasse(), requete.nom());
            return reponseAvecSession(utilisateur);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Connexion
    // ------------------------------------------------------------------
    @Operation(summary = "Se connecter et obtenir un jeton JWT",
            description = "Renvoie `{email, nom, role, token, refreshToken}`. "
                    + "Le role vaut ADMIN, CLIENT ou AGENT_SUPPORT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification reussie"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"erreur\":\"Email ou mot de passe incorrect\"}")))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> connexion(@Valid @RequestBody ConnexionRequete requete) {
        if (!authService.seConnecter(requete.email(), requete.motDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erreur", "Email ou mot de passe incorrect"));
        }

        Utilisateur utilisateur = authService.trouverParEmail(requete.email()).orElseThrow();
        return reponseAvecSession(utilisateur);
    }

    // ------------------------------------------------------------------
    // Rafraichissement du token
    // ------------------------------------------------------------------
    @Operation(summary = "Rafraichir le token JWT",
            description = "Verifie le refresh token, le fait tourner (rotation) et renvoie un nouvel access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau jeton emis"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide, expire ou revoque")
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {

        // 1. Verifier le refresh token et effectuer sa rotation
        var rotation = refreshTokenService.rotateRefreshToken(request.refreshToken());

        // 2. Utilisateur associe
        Utilisateur utilisateur = rotation.user();

        // 3. Role et nouvel access token
        String role = authService.rolePourToken(utilisateur);
        String accessToken = jwtService.creerToken(utilisateur.getEmail(), role);

        // 4. Reponse
        Map<String, String> corps = new LinkedHashMap<>();
        corps.put("email", utilisateur.getEmail());
        corps.put("role", role);
        corps.put("token", accessToken);
        corps.put("refreshToken", rotation.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, creerCookieSession(accessToken).toString())
                .body(corps);
    }

    // ------------------------------------------------------------------
    // Deconnexion (une seule route /logout)
    // ------------------------------------------------------------------
    @Operation(summary = "Se deconnecter",
            description = "Vide le cookie de session et revoque le refresh token s'il est fourni dans le corps. "
                    + "Le jeton d'acces reste valable jusqu'a expiration : le supprimer cote client.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Session fermee"))
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> deconnexion(
            @RequestBody(required = false) RefreshTokenRequest request) {

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenService.revokeRefreshToken(request.refreshToken());
        }
        authService.seDeconnecter();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, creerCookieVide().toString())
                .body(Map.of("message", "Déconnecté"));
    }

    // ------------------------------------------------------------------
    // Reinitialisation du mot de passe
    // ------------------------------------------------------------------
    /** Étape 1 : génère un code OTP. Même réponse que l'email existe ou non. */
    @Operation(summary = "Demander un code de reinitialisation du mot de passe")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> motDePasseOublie(
            @Valid @RequestBody DemandeReinitialisationRequete requete) {
        reinitialisationService.demanderReinitialisation(requete.email());
        return ResponseEntity.ok(Map.of("message",
                "Si cette adresse est enregistrée, un code de réinitialisation vient d'être envoyé."));
    }

    /** Étape 2 : vérifie le code, change le mot de passe et invalide le code. */
    @Operation(summary = "Reinitialiser le mot de passe avec le code recu")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> reinitialiserMotDePasse(
            @Valid @RequestBody ResetPasswordDTO requete) {
        reinitialisationService.reinitialiserMotDePasse(
                requete.email(), requete.code(), requete.nouveauMotDePasse());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé"));
    }

    // ------------------------------------------------------------------
    // Méthodes internes
    // ------------------------------------------------------------------
    private ResponseEntity<Map<String, String>> reponseAvecSession(Utilisateur utilisateur) {
        String role = authService.rolePourToken(utilisateur);
        String token = jwtService.creerToken(utilisateur.getEmail(), role);
        String refreshToken = refreshTokenService.createRefreshToken(utilisateur);

        Map<String, String> corps = new LinkedHashMap<>();
        corps.put("email", utilisateur.getEmail());
        corps.put("nom", utilisateur.getNom() == null ? "" : utilisateur.getNom());
        corps.put("role", role);
        corps.put("token", token);
        corps.put("refreshToken", refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, creerCookieSession(token).toString())
                .body(corps);
    }

    private ResponseCookie creerCookieSession(String token) {
        return ResponseCookie.from(COOKIE_SESSION, token)
                .httpOnly(true)
                .secure(cookieSecurise)
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getDureeValiditeMs()))
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie creerCookieVide() {
        return ResponseCookie.from(COOKIE_SESSION, "")
                .httpOnly(true)
                .secure(cookieSecurise)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    // ------------------------------------------------------------------
    // Requêtes (supprimer si ces records existent déjà dans votre projet)
    // ------------------------------------------------------------------
    public record InscriptionRequete(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String motDePasse,
            @Size(max = 100) String nom) {
    }

    public record ConnexionRequete(
            @NotBlank @Email String email,
            @NotBlank String motDePasse) {
    }

    public record DemandeReinitialisationRequete(
            @NotBlank @Email String email) {
    }
}