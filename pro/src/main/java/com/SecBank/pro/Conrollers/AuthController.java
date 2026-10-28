package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Configurations.JwtService;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Services.AuthService;
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

/**
 * Endpoints publics d'authentification.
 * Le token est renvoyé dans un cookie httpOnly "session" (attendu par le frontend).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_SESSION = "session";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

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

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> connexion(@RequestBody ConnexionRequete requete) {
        if (!authService.seConnecter(requete.email(), requete.motDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erreur", "Email ou mot de passe incorrect"));
        }

        Utilisateur utilisateur = authService.trouverParEmail(requete.email()).orElseThrow();
        return reponseAvecSession(utilisateur);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> deconnexion() {
        authService.seDeconnecter();
        ResponseCookie cookieVide = ResponseCookie.from(COOKIE_SESSION, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieVide.toString())
                .body(Map.of("message", "Déconnecté"));
    }

    private ResponseEntity<Map<String, String>> reponseAvecSession(Utilisateur utilisateur) {
        String role = authService.rolePourToken(utilisateur);
        String token = jwtService.creerToken(utilisateur.getEmail(), role);

        ResponseCookie cookie = ResponseCookie.from(COOKIE_SESSION, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getDureeValiditeMs()))
                .sameSite("Lax")
                .build();

        Map<String, String> corps = new LinkedHashMap<>();
        corps.put("email", utilisateur.getEmail());
        corps.put("nom", utilisateur.getNom() == null ? "" : utilisateur.getNom());
        corps.put("role", role);
        corps.put("token", token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(corps);
    }

    public record InscriptionRequete(String email, String motDePasse, String nom) {
    }

    public record ConnexionRequete(String email, String motDePasse) {
    }
}
