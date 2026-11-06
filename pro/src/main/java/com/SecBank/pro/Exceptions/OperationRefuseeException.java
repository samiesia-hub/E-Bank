package com.SecBank.pro.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 : montant invalide, solde insuffisant ou compte non operable.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OperationRefuseeException extends RuntimeException {
    public OperationRefuseeException(String message) {
        super(message);
    }
}
