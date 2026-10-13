package com.legrandcinema.exception;

import com.legrandcinema.dto.response.ErreurResponse;
import com.legrandcinema.entity.Place;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler gestionnaire = new GlobalExceptionHandler();

    @Test
    void gererConflitDeVersion_renvoie409AvecMessageClair() {
        ObjectOptimisticLockingFailureException exception = new ObjectOptimisticLockingFailureException(Place.class, 1L);

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererConflitDeVersion(exception);

        assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals(409, reponse.getBody().getStatut());
        assertEquals("Cette place vient d'être modifiée par un autre utilisateur, merci de recharger les places et de réessayer",
                reponse.getBody().getMessage());
    }
}