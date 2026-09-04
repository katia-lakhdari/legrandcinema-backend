package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Place.StatutPlace;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Reservation.StatutReservation;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.dto.request.ReservationRequest;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.SeanceRepository;
import com.legrandcinema.repository.UtilisateurRepository;
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
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Utilisateur utilisateur;
    private Seance seance;
    private Place place;
    private static final String EMAIL = "katia@legrandcinema.com";

    @BeforeEach
    void initialisation() {
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail(EMAIL);

        seance = new Seance();
        seance.setId(10L);

        place = new Place();
        place.setId(100L);
        place.setNumero("A12");
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(3));
        place.setUtilisateurVerrouillage(utilisateur);
    }

    @Test
    void creerReservation_placeValide_creeReservationAvecSucces() {
        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(100L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(place));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        Reservation resultat = reservationService.creerReservation(requete, EMAIL);

        assertEquals(StatutReservation.EN_ATTENTE_PAIEMENT, resultat.getStatut());
        assertEquals(StatutPlace.RESERVEE, place.getStatut());
        assertNull(place.getFinVerrouillage());
        assertNull(place.getUtilisateurVerrouillage());
        verify(reservationRepository).save(any(Reservation.class));
        verify(placeRepository).save(place);
    }

    @Test
    void creerReservation_seanceInexistante_leveResourceNotFoundException() {
        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(99L);
        requete.setPlaceIds(List.of(100L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_placeInexistante_leveResourceNotFoundException() {
        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(999L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_placeNonVerrouillee_leveException() {
        place.setStatut(StatutPlace.LIBRE);

        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(100L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        assertEquals("Cette place n'est pas verrouillée", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_placeVerrouilleeParAutreUtilisateur_leveException() {
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(2L);
        place.setUtilisateurVerrouillage(autreUtilisateur);

        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(100L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        assertEquals("Cette place est verrouillée par un autre utilisateur", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_verrouillageExpire_leveException() {
        place.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));

        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(100L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(place));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        assertEquals("Le verrouillage de cette place a expiré", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerReservation_unePlaceInvalideParmiDeux_neSauvegardeRien() {
        Place placeValide = place;

        Place placeInvalide = new Place();
        placeInvalide.setId(101L);
        placeInvalide.setNumero("A13");
        placeInvalide.setStatut(StatutPlace.LIBRE);

        ReservationRequest requete = new ReservationRequest();
        requete.setSeanceId(10L);
        requete.setPlaceIds(List.of(100L, 101L));

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(seanceRepository.findById(10L)).thenReturn(Optional.of(seance));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(placeValide));
        when(placeRepository.findById(101L)).thenReturn(Optional.of(placeInvalide));

        assertThrows(RuntimeException.class,
                () -> reservationService.creerReservation(requete, EMAIL));

        verify(reservationRepository, never()).save(any());
        verify(placeRepository, never()).save(any());
    }
}