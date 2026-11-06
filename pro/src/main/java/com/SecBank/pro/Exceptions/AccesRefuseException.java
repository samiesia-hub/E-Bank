package com.SecBank.pro.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 403 : le client connecte demande une ressource dont il n'est pas proprietaire.
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException(String message) {
        super(message);
    }
}
