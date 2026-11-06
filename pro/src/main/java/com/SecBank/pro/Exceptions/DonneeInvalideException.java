package com.SecBank.pro.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 : saisie admin invalide (solde initial negatif, transition de statut interdite...).
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DonneeInvalideException extends RuntimeException {
    public DonneeInvalideException(String message) {
        super(message);
    }
}
