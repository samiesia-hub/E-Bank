package com.SecBank.pro.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DÉVELOPPEMENT UNIQUEMENT : aucun envoi d'email n'est branché pour l'instant,
 * le code est écrit dans la console du serveur.
 * À remplacer par une implémentation qui envoie un vrai email.
 */
@Slf4j
@Service
public class EnvoiCodeConsoleService implements EnvoiCodeService {

    @Override
    public void envoyerCodeReinitialisation(String email, String code) {
        log.warn("[DEV] Code de réinitialisation pour {} : {} (valable 10 minutes)", email, code);
    }
}
