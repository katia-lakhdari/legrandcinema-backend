package com.legrandcinema.exception;

import com.legrandcinema.dto.response.ErreurResponse;
import com.legrandcinema.entity.Place;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

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

    @Test
    void gererRouteInconnue_renvoie404() {
        NoResourceFoundException exception = mock(NoResourceFoundException.class);

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererRouteInconnue(exception);

        assertEquals(HttpStatus.NOT_FOUND, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals("Cette adresse n'existe pas", reponse.getBody().getMessage());
    }

    @Test
    void gererFormatNonAccepte_renvoie415() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException("text/plain non accepté");

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererFormatNonAccepte(exception);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals(415, reponse.getBody().getStatut());
    }

    @Test
    void gererParametreManquant_renvoie400AvecLeNomDuParametre() {
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException("seanceId", "Long");

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererParametreManquant(exception);

        assertEquals(HttpStatus.BAD_REQUEST, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals("Le paramètre seanceId est obligatoire", reponse.getBody().getMessage());
    }

    @Test
    void gererDonneesEnConflit_renvoie409() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate entry");

        ResponseEntity<ErreurResponse> reponse = gestionnaire.gererDonneesEnConflit(exception);

        assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode());
        assertNotNull(reponse.getBody());
        assertEquals("Cette opération entre en conflit avec des données existantes", reponse.getBody().getMessage());
    }
}

