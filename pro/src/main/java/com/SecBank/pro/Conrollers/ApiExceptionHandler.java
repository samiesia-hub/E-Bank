package com.SecBank.pro.Conrollers;

import com.SecBank.pro.Exceptions.AccesRefuseException;
import com.SecBank.pro.Exceptions.DonneeInvalideException;
import com.SecBank.pro.Exceptions.OperationRefuseeException;
import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Exceptions.RibDejaUtiliseException;
import com.SecBank.pro.Exceptions.RibInvalideException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduit les refus metier en reponses 400 / 403 / 404 / 409 avec un message
 * exploitable. Sans cela, le corps d'erreur par defaut de Spring n'expose pas
 * le motif.
 *
 * Toute exception inattendue (bug, panne base) retourne un 500 generique : la
 * stack trace reste dans les logs serveur et ne fuit jamais vers le client.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Message unique des 500 : aucune donnee technique ni metier n'y figure. */
    static final String MESSAGE_ERREUR_INTERNE = "Erreur interne du serveur, réessayer plus tard";

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> introuvable(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<Map<String, String>> accesRefuse(AccesRefuseException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }

    /**
     * Filet de securite. Les exceptions deja qualifiees par Spring (400 de
     * validation, 404 de ressource inconnue, 415...) exposent leur propre statut
     * et gardent leur corps standard ; le reste devient un 500 generique.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> erreurInattendue(Exception exception, HttpServletRequest requete) {
        if (exception instanceof ErrorResponse erreur) {
            return ResponseEntity.status(erreur.getStatusCode()).body(erreur.getBody());
        }
        log.error("Erreur inattendue sur {} {}", requete.getMethod(), requete.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", MESSAGE_ERREUR_INTERNE));
    }
}
