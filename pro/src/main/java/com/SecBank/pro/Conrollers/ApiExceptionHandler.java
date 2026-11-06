package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.RibDejaUtiliseException;
import com.SecBank.pro.Exceptions.RibInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduit les refus metier en reponses 400 / 409 avec un message exploitable.
 * Sans cela, le corps d'erreur par defaut de Spring n'expose pas le motif.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OperationRefuseeException.class)
    public ResponseEntity<Map<String, String>> operationRefusee(OperationRefuseeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DonneeInvalideException.class)
    public ResponseEntity<Map<String, String>> donneeInvalide(DonneeInvalideException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RibInvalideException.class)
    public ResponseEntity<Map<String, String>> ribInvalide(RibInvalideException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RibDejaUtiliseException.class)
    public ResponseEntity<Map<String, String>> ribDejaUtilise(RibDejaUtiliseException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}
