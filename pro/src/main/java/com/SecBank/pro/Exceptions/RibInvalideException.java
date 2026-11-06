package com.SecBank.pro.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 400 : RIB saisi dont le format ou la cle de controle est invalide.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RibInvalideException extends RuntimeException {
    public RibInvalideException(String message) {
        super(message);
    }
}
