package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Place.StatutPlace;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Reservation.StatutReservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpirationReservationServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaiementService paiementService;

    @InjectMocks
    private ExpirationReservationService expirationReservationService;

    private Utilisateur client;

    @BeforeEach
    void initialisation() {
        client = new Utilisateur();
        client.setId(1L);
        client.setEmail("katia@legrandcinema.com");
    }

    private Place creerPlaceExpiree(Long id, Reservation reservation) {
        Place place = new Place();
        place.setId(id);
        place.setNumero("A" + id);
        place.setStatut(StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
        place.setUtilisateurVerrouillage(client);
        place.setReservation(reservation);
        return place;
    }

    private Reservation creerReservation(Long id, StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUtilisateur(client);
        reservation.setStatut(statut);
        reservation.setPaymentIntentId("pi_test_" + id);
        return reservation;
    }

    private void verifierPlaceLiberee(Place place) {
        assertEquals(StatutPlace.LIBRE, place.getStatut());
        assertNull(place.getFinVerrouillage());
        assertNull(place.getUtilisateurVerrouillage());
        assertNull(place.getReservation());
    }

    @Test
    void libererVerrousExpires_aucunePlaceExpiree_neFaitRien() {
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int resultat = expirationReservationService.libererVerrousExpires();

        assertEquals(0, resultat);
        verify(placeRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
        verify(paiementService, never()).annulerOuRembourserIntention(any());
    }

    @Test
    void libererVerrousExpires_requete_utiliseLeStatutVerrouilleeEtLHeureActuelle() {
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        LocalDateTime avant = LocalDateTime.now();
        expirationReservationService.libererVerrousExpires();
        LocalDateTime apres = LocalDateTime.now();

        verify(placeRepository).findByStatutAndFinVerrouillageBefore(
                eq(StatutPlace.VERROUILLEE),
                argThat((LocalDateTime date) -> !date.isBefore(avant) && !date.isAfter(apres)));
    }

    @Test
    void libererVerrousExpires_placeSansReservation_estLibereeSansAnnulation() {
        Place place = creerPlaceExpiree(100L, null);
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of(place));

        int resultat = expirationReservationService.libererVerrousExpires();

        assertEquals(1, resultat);
        verifierPlaceLiberee(place);
        verify(placeRepository, times(1)).save(place);
        verify(reservationRepository, never()).save(any());
        verify(paiementService, never()).annulerOuRembourserIntention(any());
    }

    @Test
    void libererVerrousExpires_deuxPlacesDUneMemeReservation_annuleeUneSeuleFois() {
        Reservation reservation = creerReservation(10L, StatutReservation.EN_ATTENTE_PAIEMENT);
        Place place1 = creerPlaceExpiree(100L, reservation);
        Place place2 = creerPlaceExpiree(101L, reservation);
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of(place1, place2));

        int resultat = expirationReservationService.libererVerrousExpires();

        assertEquals(2, resultat);
        verifierPlaceLiberee(place1);
        verifierPlaceLiberee(place2);
        assertEquals(StatutReservation.ANNULEE, reservation.getStatut());
        verify(placeRepository, times(2)).save(any(Place.class));
        verify(reservationRepository, times(1)).save(reservation);
        verify(paiementService, times(1)).annulerOuRembourserIntention(reservation);
    }

    @Test
    void libererVerrousExpires_deuxReservationsDifferentes_chacuneAnnulee() {
        Reservation reservation1 = creerReservation(10L, StatutReservation.EN_ATTENTE_PAIEMENT);
        Reservation reservation2 = creerReservation(20L, StatutReservation.EN_ATTENTE_PAIEMENT);
        Place place1 = creerPlaceExpiree(100L, reservation1);
        Place place2 = creerPlaceExpiree(200L, reservation2);
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of(place1, place2));

        int resultat = expirationReservationService.libererVerrousExpires();

        assertEquals(2, resultat);
        assertEquals(StatutReservation.ANNULEE, reservation1.getStatut());
        assertEquals(StatutReservation.ANNULEE, reservation2.getStatut());
        verify(reservationRepository, times(1)).save(reservation1);
        verify(reservationRepository, times(1)).save(reservation2);
        verify(paiementService, times(1)).annulerOuRembourserIntention(reservation1);
        verify(paiementService, times(1)).annulerOuRembourserIntention(reservation2);
    }

    @Test
    void libererVerrousExpires_reservationDejaAnnulee_placeLibereeSansRappelerStripe() {
        Reservation reservation = creerReservation(10L, StatutReservation.ANNULEE);
        Place place = creerPlaceExpiree(100L, reservation);
        when(placeRepository.findByStatutAndFinVerrouillageBefore(eq(StatutPlace.VERROUILLEE), any(LocalDateTime.class)))
                .thenReturn(List.of(place));

        int resultat = expirationReservationService.libererVerrousExpires();

        assertEquals(1, resultat);
        verifierPlaceLiberee(place);
        assertEquals(StatutReservation.ANNULEE, reservation.getStatut());
        verify(reservationRepository, never()).save(any());
        verify(paiementService, never()).annulerOuRembourserIntention(any());
    }
}