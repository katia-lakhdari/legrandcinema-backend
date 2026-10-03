package com.legrandcinema.exception;

import com.legrandcinema.dto.response.ErreurResponse;
import com.legrandcinema.dto.response.ErreurValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErreurResponse> gererRessourceIntrouvable(ResourceNotFoundException exception) {
        ErreurResponse erreur = new ErreurResponse(exception.getMessage(), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(erreur, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurValidationResponse> gererErreurValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        for (FieldError erreurChamp : exception.getBindingResult().getFieldErrors()) {
            erreurs.putIfAbsent(erreurChamp.getField(), erreurChamp.getDefaultMessage());
        }

        ErreurValidationResponse reponse = new ErreurValidationResponse(
                "Données invalides",
                HttpStatus.BAD_REQUEST.value(),
                erreurs
        );
        return new ResponseEntity<>(reponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErreurResponse> gererErreurMetier(RuntimeException exception) {
        ErreurResponse erreur = new ErreurResponse(exception.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(erreur, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurResponse> gererErreurInattendue(Exception exception) {
        ErreurResponse erreur = new ErreurResponse("Une erreur inattendue est survenue", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(erreur, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}