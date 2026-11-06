package com.SecBank.pro.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 409 : RIB (ou numero de compte) deja attribue a un autre compte.
@ResponseStatus(HttpStatus.CONFLICT)
public class RibDejaUtiliseException extends RuntimeException {
    public RibDejaUtiliseException(String message) {
        super(message);
    }
}
