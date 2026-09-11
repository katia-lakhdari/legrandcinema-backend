package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Place.StatutPlace;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private PlaceService placeService;

    private Place place;
    private Utilisateur utilisateur;

    @BeforeEach
    void initialisation() {
        place = new Place();
        place.setId(1L);
        place.setNumero("A12");
        place.setStatut(StatutPlace.LIBRE);

        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail("katia@legrandcinema.com");
    }

    @Test
    void verrouillerPlace_placeLibre_verrouilleAvecSucces() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.verrouillerPlace(1L, "katia@legrandcinema.com");

        assertEquals(StatutPlace.VERROUILLEE, resultat.getStatut());
        assertNotNull(resultat.getFinVerrouillage());
        assertTrue(resultat.getFinVerrouillage().isAfter(LocalDateTime.now()));
        assertEquals(utilisateur, resultat.getUtilisateurVerrouillage());
    }

    @Test
    void verrouillerPlace_placeReservee_leveException() {
        place.setStatut(StatutPlace.RESERVEE);
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.verrouillerPlace(1L, "katia@legrandcinema.com"));

        assertEquals("Cette place est déjà réservée", exception.getMessage());
        verify(placeRepository, never()).save(any());
    }

    @Test
    void verrouillerPlace_placeVerrouilleeNonExpiree_leveException() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(3));
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.verrouillerPlace(1L, "katia@legrandcinema.com"));

        assertEquals("Cette place est déjà en cours de sélection par un autre client", exception.getMessage());
        verify(placeRepository, never()).save(any());
    }

    @Test
    void verrouillerPlace_placeVerrouilleeExpiree_verrouilleAvecSucces() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.verrouillerPlace(1L, "katia@legrandcinema.com");

        assertEquals(StatutPlace.VERROUILLEE, resultat.getStatut());
        assertTrue(resultat.getFinVerrouillage().isAfter(LocalDateTime.now()));
    }

    @Test
    void verrouillerPlace_placeInexistante_leveResourceNotFoundException() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> placeService.verrouillerPlace(99L, "katia@legrandcinema.com"));
    }

    @Test
    void libererPlace_placeVerrouillee_repasseLibre() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(2));
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.libererPlace(1L);

        assertEquals(StatutPlace.LIBRE, resultat.getStatut());
        assertNull(resultat.getFinVerrouillage());
    }

    @Test
    void listerPlacesParSeance_placeExpiree_estLibereeAutomatiquement() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
        when(placeRepository.findBySeanceId(10L)).thenReturn(List.of(place));

        List<Place> resultat = placeService.listerPlacesParSeance(10L);

        assertEquals(StatutPlace.LIBRE, resultat.get(0).getStatut());
        verify(placeRepository).save(place);
    }

    @Test
    void bloquerPlace_placeLibre_bloqueAvecSucces() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.bloquerPlace(1L, "Siège défectueux");

        assertEquals(StatutPlace.BLOQUEE, resultat.getStatut());
        assertEquals("Siège défectueux", resultat.getRaisonBlocage());
    }

    @Test
    void bloquerPlace_placeReservee_leveException() {
        place.setStatut(StatutPlace.RESERVEE);
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.bloquerPlace(1L, "Siège défectueux"));

        assertEquals("Impossible de bloquer une place déjà réservée", exception.getMessage());
        verify(placeRepository, never()).save(any());
    }

    @Test
    void bloquerPlace_placeVerrouilleeNonExpiree_leveException() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(3));
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.bloquerPlace(1L, "Siège défectueux"));

        assertEquals("Impossible de bloquer une place en cours de sélection par un client", exception.getMessage());
        verify(placeRepository, never()).save(any());
    }

    @Test
    void bloquerPlace_placeVerrouilleeExpiree_bloqueAvecSucces() {
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.bloquerPlace(1L, "Siège cassé");

        assertEquals(StatutPlace.BLOQUEE, resultat.getStatut());
        assertEquals("Siège cassé", resultat.getRaisonBlocage());
    }

    @Test
    void bloquerPlace_placeInexistante_leveResourceNotFoundException() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> placeService.bloquerPlace(99L, "Siège défectueux"));
    }

    @Test
    void debloquerPlace_placeBloquee_repasseLibre() {
        place.setStatut(StatutPlace.BLOQUEE);
        place.setRaisonBlocage("Siège défectueux");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Place resultat = placeService.debloquerPlace(1L);

        assertEquals(StatutPlace.LIBRE, resultat.getStatut());
        assertNull(resultat.getRaisonBlocage());
    }

    @Test
    void debloquerPlace_placeNonBloquee_leveException() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.debloquerPlace(1L));

        assertEquals("Cette place n'est pas bloquée", exception.getMessage());
        verify(placeRepository, never()).save(any());
    }
}