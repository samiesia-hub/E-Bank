package com.SecBank.pro.Conrollers.Clients;

import com.SecBank.pro.Dtos.ChangePasswordRequestDTO;
import com.SecBank.pro.Dtos.ModifierProfilRequest;
import com.SecBank.pro.Dtos.ProfilClientResponse;
import com.SecBank.pro.Services.Clients.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Profil du client connecté. Le client est identifié par l'email du token JWT,
 * jamais par un identifiant transmis par l'appelant.
 */
@RestController
public class ClientProfileController {

    private final ClientService clientService;

    public ClientProfileController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/api/client/profile")
    public ResponseEntity<ProfilClientResponse> monProfil(Authentication authentication) {
        return ResponseEntity.ok(
                ProfilClientResponse.depuis(clientService.consulterProfil(authentication.getName())));
    }

    @PutMapping("/api/client/profile")
    public ResponseEntity<ProfilClientResponse> modifierMonProfil(@RequestBody ModifierProfilRequest requete,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(
                ProfilClientResponse.depuis(clientService.modifierProfil(authentication.getName(), requete)));
    }

    @PostMapping("/api/client/change-password")
    public ResponseEntity<Map<String, String>> changerMotDePasse(@RequestBody ChangePasswordRequestDTO requete,
                                                                 Authentication authentication) {
        clientService.changerMotDePasse(authentication.getName(), requete);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié"));
    }
}