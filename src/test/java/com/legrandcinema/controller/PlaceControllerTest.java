package com.legrandcinema.controller;

import com.legrandcinema.dto.request.BlocagePlaceRequest;
import com.legrandcinema.dto.response.PlaceResponse;
import com.legrandcinema.entity.Place;
import com.legrandcinema.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

    @Mock
    private PlaceService placeService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PlaceController placeController;

    private Place creerPlaceDeTest(Long id, String numero, Place.StatutPlace statut) {
        Place place = new Place();
        place.setId(id);
        place.setNumero(numero);
        place.setStatut(statut);
        return place;
    }

    @Test
    void listerPlacesParSeance_deuxPlaces_retourneDeuxPlaceResponse() {
        when(placeService.listerPlacesParSeance(12L)).thenReturn(List.of(
                creerPlaceDeTest(1L, "A1", Place.StatutPlace.LIBRE),
                creerPlaceDeTest(2L, "A2", Place.StatutPlace.RESERVEE)));

        List<PlaceResponse> resultat = placeController.listerPlacesParSeance(12L);

        assertEquals(2, resultat.size());
        PlaceResponse premierePlace = resultat.get(0);
        assertEquals(1L, premierePlace.getId());
        assertEquals("A1", premierePlace.getNumero());
        assertEquals("LIBRE", premierePlace.getStatut());
        assertNull(premierePlace.getFinVerrouillage());
        assertNull(premierePlace.getRaisonBlocage());
        assertEquals("RESERVEE", resultat.get(1).getStatut());
    }

    @Test
    void verrouillerPlace_clientConnecte_retourneLaPlaceVerrouillee() {
        LocalDateTime finVerrouillage = LocalDateTime.of(2026, 11, 2, 20, 10);
        Place place = creerPlaceDeTest(1L, "A1", Place.StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(finVerrouillage);
        when(authentication.getName()).thenReturn("client@test.fr");
        when(placeService.verrouillerPlace(1L, "client@test.fr")).thenReturn(place);

        PlaceResponse resultat = placeController.verrouillerPlace(1L, authentication);

        assertEquals("VERROUILLEE", resultat.getStatut());
        assertEquals(finVerrouillage, resultat.getFinVerrouillage());
    }

    @Test
    void libererPlace_clientConnecte_retourneLaPlaceLibre() {
        when(authentication.getName()).thenReturn("client@test.fr");
        when(placeService.libererPlace(1L, "client@test.fr"))
                .thenReturn(creerPlaceDeTest(1L, "A1", Place.StatutPlace.LIBRE));

        PlaceResponse resultat = placeController.libererPlace(1L, authentication);

        assertEquals("LIBRE", resultat.getStatut());
    }

    @Test
    void bloquerPlace_raisonDonnee_retourneLaPlaceBloqueeAvecSaRaison() {
        BlocagePlaceRequest requete = new BlocagePlaceRequest();
        requete.setRaison("Fauteuil cassé");
        Place place = creerPlaceDeTest(1L, "A1", Place.StatutPlace.BLOQUEE);
        place.setRaisonBlocage("Fauteuil cassé");
        when(placeService.bloquerPlace(1L, "Fauteuil cassé")).thenReturn(place);

        PlaceResponse resultat = placeController.bloquerPlace(1L, requete);

        assertEquals("BLOQUEE", resultat.getStatut());
        assertEquals("Fauteuil cassé", resultat.getRaisonBlocage());
    }

    @Test
    void debloquerPlace_placeBloquee_retourneLaPlaceLibre() {
        when(placeService.debloquerPlace(1L)).thenReturn(creerPlaceDeTest(1L, "A1", Place.StatutPlace.LIBRE));

        PlaceResponse resultat = placeController.debloquerPlace(1L);

        assertEquals("LIBRE", resultat.getStatut());
        assertNull(resultat.getRaisonBlocage());
    }
}
