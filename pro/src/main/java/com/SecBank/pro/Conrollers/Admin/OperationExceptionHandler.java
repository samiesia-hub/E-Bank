package com.SecBank.pro.Conrollers.Admin;

import com.SecBank.pro.Exceptions.OperationRefuseeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Les operations refusees doivent renvoyer 400 avec un message exploitable
 * par le client, et non une page d'erreur generique sans explication.
 */
@RestControllerAdvice
public class OperationExceptionHandler {

    @ExceptionHandler(OperationRefuseeException.class)
    public ResponseEntity<Map<String, String>> operationRefusee(OperationRefuseeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}
