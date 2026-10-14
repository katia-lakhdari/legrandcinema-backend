package com.legrandcinema.exception;

import com.legrandcinema.dto.response.ErreurResponse;
import com.legrandcinema.dto.response.ErreurValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErreurResponse> gererRessourceIntrouvable(ResourceNotFoundException exception) {
        return construireReponse(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ErreurResponse> gererConflit(ConflitException exception) {
        return construireReponse(exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErreurResponse> gererConflitDeVersion(ObjectOptimisticLockingFailureException exception) {
        return construireReponse("Cette place vient d'être modifiée par un autre utilisateur, merci de recharger les places et de réessayer", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ResponseEntity<ErreurResponse> gererRequeteInvalide(RequeteInvalideException exception) {
        return construireReponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurResponse> gererIdentifiantInvalide(MethodArgumentTypeMismatchException exception) {
        return construireReponse("Identifiant invalide", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurResponse> gererCorpsIllisible(HttpMessageNotReadableException exception) {
        return construireReponse("Corps de la requête invalide ou mal formé", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErreurResponse> gererIdentifiantsIncorrects(BadCredentialsException exception) {
        return construireReponse(exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErreurResponse> gererAccesRefuse(AccessDeniedException exception) {
        return construireReponse(exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ErreurTechniqueException.class)
    public ResponseEntity<ErreurResponse> gererErreurTechnique(ErreurTechniqueException exception) {
        LOGGER.error("Erreur technique : {}", exception.getMessage(), exception);
        return construireReponse(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurResponse> gererMethodeNonAutorisee(HttpRequestMethodNotSupportedException exception) {
        String message = "La méthode " + exception.getMethod() + " n'est pas autorisée pour cette route";
        ErreurResponse erreur = new ErreurResponse(message, HttpStatus.METHOD_NOT_ALLOWED.value());

        HttpHeaders entetes = new HttpHeaders();
        Set<HttpMethod> methodesAutorisees = exception.getSupportedHttpMethods();
        if (methodesAutorisees != null) {
            entetes.setAllow(methodesAutorisees);
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(entetes)
                .body(erreur);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurResponse> gererErreurInattendue(Exception exception) {
        LOGGER.error("Erreur inattendue", exception);
        return construireReponse("Une erreur inattendue est survenue", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErreurResponse> construireReponse(String message, HttpStatus statut) {
        ErreurResponse erreur = new ErreurResponse(message, statut.value());
        return new ResponseEntity<>(erreur, statut);
    }
}