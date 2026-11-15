package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Dtos.RefreshTokenRequest;
import com.SecBank.pro.Dtos.ResetPasswordDTO;
import com.SecBank.pro.Dtos.TokenResponse;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Services.AuthService;
import com.SecBank.pro.Services.RefreshTokenService;
import com.SecBank.pro.Services.ReinitialisationMotDePasseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints publics d'authentification.
 * Le token est renvoyé dans un cookie httpOnly "session" (attendu par le frontend).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion et deconnexion. Routes publiques.")
public class AuthController {

    private static final String COOKIE_SESSION = "session";
    private static final String COOKIE_REFRESH = "refresh";

    private final AuthService authService;
    private final JwtService jwtService;
    private final ReinitialisationMotDePasseService reinitialisationService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          ReinitialisationMotDePasseService reinitialisationService,
                          RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.reinitialisationService = reinitialisationService;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "Inscrire un nouveau client",
            description = "Cree le client puis ouvre une session : jeton JWT dans le corps (`token`) et cookie httpOnly `session`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte cree, jeton renvoye"),
            @ApiResponse(responseCode = "400", description = "Email deja utilise ou donnees invalides",
                    content = @Content(examples = @ExampleObject(value = "{\"erreur\":\"Email deja utilise\"}")))
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> inscription(@RequestBody InscriptionRequete requete) {
        try {
            Utilisateur utilisateur = authService.inscrire(
                    requete.email(), requete.motDePasse(), requete.nom());
            return reponseAvecSession(utilisateur);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @Operation(summary = "Se connecter et obtenir un jeton JWT",
            description = "Renvoie `{email, nom, role, token}`. Le role vaut ADMIN, CLIENT ou AGENT_SUPPORT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification reussie"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"erreur\":\"Email ou mot de passe incorrect\"}")))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> connexion(@RequestBody ConnexionRequete requete) {
        if (!authService.seConnecter(requete.email(), requete.motDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erreur", "Email ou mot de passe incorrect"));
        }

        Utilisateur utilisateur = authService.trouverParEmail(requete.email()).orElseThrow();
        return reponseAvecSession(utilisateur);
    }

    @Operation(summary = "Se deconnecter",
            description = "Vide le cookie de session. Le jeton reste valable jusqu'a expiration cote client : le supprimer cote appelant.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Session fermee"))
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> deconnexion(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshDuCookie) {
        authService.seDeconnecter();
        if (refreshDuCookie != null && !refreshDuCookie.isBlank()) {
            try {
                refreshTokenService.revokeRefreshToken(refreshDuCookie);
            } catch (ResponseStatusException ignore) {
                // Jeton deja expire ou inconnu : la deconnexion reste valable.
            }
        }
        ResponseCookie cookieVide = ResponseCookie.from(COOKIE_SESSION, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        ResponseCookie refreshVide = ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieVide.toString(), refreshVide.toString())
                .body(Map.of("message", "Déconnecté"));
    }

    /** Étape 1 : génère un code OTP. Même réponse que l'email existe ou non. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> motDePasseOublie(@RequestBody DemandeReinitialisationRequete requete) {
        reinitialisationService.demanderReinitialisation(requete.email());
        return ResponseEntity.ok(Map.of("message",
                "Si cette adresse est enregistrée, un code de réinitialisation vient d'être envoyé."));
    }

    /** Étape 2 : vérifie le code, change le mot de passe et invalide le code. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> reinitialiserMotDePasse(@RequestBody ResetPasswordDTO requete) {
        reinitialisationService.reinitialiserMotDePasse(
                requete.email(), requete.code(), requete.nouveauMotDePasse());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé"));
    }

    @Operation(summary = "Renouveler un jeton d'acces",
            description = "Echange un refresh token valide contre un nouveau jeton d'acces et un nouveau "
                    + "refresh token (rotation : l'ancien est revoque). Le refresh token peut venir du corps "
                    + "ou du cookie httpOnly `refresh`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveaux jetons emis"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide, revoque ou expire"),
            @ApiResponse(responseCode = "403", description = "Compte suspendu")
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<?> rafraichir(
            @RequestBody(required = false) RefreshTokenRequest requete,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshDuCookie) {
        String brut = requete != null && requete.refreshToken() != null && !requete.refreshToken().isBlank()
                ? requete.refreshToken()
                : refreshDuCookie;

        if (brut == null || brut.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erreur", "Refresh Token manquant"));
        }

        try {
            RefreshTokenService.TokenRotation rotation = refreshTokenService.rotateRefreshToken(brut);
            if (!rotation.actif()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("erreur", "Compte suspendu"));
            }

            Utilisateur utilisateur = authService.trouverParEmail(rotation.email()).orElseThrow();
            String role = authService.rolePourToken(utilisateur);
            String token = jwtService.creerToken(utilisateur.getEmail(), role);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,
                            cookieSession(token).toString(),
                            cookieRefresh(rotation.refreshToken()).toString())
                    .body(new TokenResponse(token, rotation.refreshToken(), "Bearer",
                            jwtService.getDureeValiditeMs() / 1000));
        } catch (ResponseStatusException e) {
            String motif = e.getReason() == null ? "Refresh Token invalide" : e.getReason();
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("erreur", motif));
        }
    }

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
                .header(HttpHeaders.SET_COOKIE,
                        cookieSession(token).toString(),
                        cookieRefresh(refreshToken).toString())
                .body(corps);
    }

    private ResponseCookie cookieSession(String token) {
        return ResponseCookie.from(COOKIE_SESSION, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getDureeValiditeMs()))
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie cookieRefresh(String refreshToken) {
        return ResponseCookie.from(COOKIE_REFRESH, refreshToken)
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
    }

    @Schema(name = "InscriptionRequete", description = "Donnees d'inscription")
    public record InscriptionRequete(
            @Schema(example = "nouveau.client@demo.bankati.test") String email,
            @Schema(example = "Client1234!") String motDePasse,
            @Schema(example = "Nouveau Client") String nom) {
    }

    @Schema(name = "ConnexionRequete", description = "Identifiants de connexion")
    public record ConnexionRequete(
            @Schema(example = "client.demo1@demo.bankati.test") String email,
            @Schema(example = "Client1234!") String motDePasse) {
    }

    public record DemandeReinitialisationRequete(String email) {
    }
}