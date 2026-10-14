package com.legrandcinema.exception;

import com.legrandcinema.dto.response.ErreurResponse;
import com.legrandcinema.entity.Place;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.Set;

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

    @Test
    void gererMethodeNonAutorisee_renvoie405AvecMessageEtEnteteAllow() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE", List.of("POST"));

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererMethodeNonAutorisee(exception);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals(405, reponse.getBody().getStatut());
        assertEquals("La méthode DELETE n'est pas autorisée pour cette route", reponse.getBody().getMessage());
        assertEquals(Set.of(HttpMethod.POST), reponse.getHeaders().getAllow());
    }
}

