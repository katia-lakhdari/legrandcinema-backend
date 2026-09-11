package com.legrandcinema.service;

import com.legrandcinema.dto.response.ReservationAdminResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationAdminServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationAdminService reservationAdminService;

    private Utilisateur utilisateur;
    private Seance seance;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setPrenom("Katia");
        utilisateur.setNom("Lakhdari");

        Film film = new Film();
        film.setTitre("Interstellar");

        seance = new Seance();
        seance.setId(1L);
        seance.setFilm(film);
        seance.setDateHeure(LocalDateTime.of(2026, 9, 15, 20, 0));
    }

    @Test
    void listerReservations_casNormal_mappeCorrectement() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUtilisateur(utilisateur);
        reservation.setSeance(seance);
        reservation.setPlaces(Arrays.asList(new Place(), new Place()));
        reservation.setStatut(Reservation.StatutReservation.PAYEE);

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        List<ReservationAdminResponse> resultats = reservationAdminService.listerReservations();

        ReservationAdminResponse resultat = resultats.get(0);
        assertEquals(1, resultats.size());
        assertEquals(1L, resultat.getReservationId());
        assertEquals("Katia Lakhdari", resultat.getClientNom());
        assertEquals("Interstellar", resultat.getFilmTitre());
        assertEquals(2, resultat.getNombrePlaces());
        assertEquals("PAYEE", resultat.getStatut());
    }

    @Test
    void listerReservations_plusieursReservations_renvoieUneEntreeParReservation() {
        Reservation reservation1 = new Reservation();
        reservation1.setId(1L);
        reservation1.setUtilisateur(utilisateur);
        reservation1.setSeance(seance);
        reservation1.setPlaces(List.of(new Place()));
        reservation1.setStatut(Reservation.StatutReservation.PAYEE);

        Utilisateur utilisateur2 = new Utilisateur();
        utilisateur2.setPrenom("Lucas");
        utilisateur2.setNom("Rinaldi");

        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setUtilisateur(utilisateur2);
        reservation2.setSeance(seance);
        reservation2.setPlaces(Collections.emptyList());
        reservation2.setStatut(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT);

        when(reservationRepository.findAll()).thenReturn(Arrays.asList(reservation1, reservation2));

        List<ReservationAdminResponse> resultats = reservationAdminService.listerReservations();

        assertEquals(2, resultats.size());
        assertEquals(1L, resultats.get(0).getReservationId());
        assertEquals(2L, resultats.get(1).getReservationId());
    }

    @Test
    void listerReservations_aucuneReservation_renvoieListeVide() {
        when(reservationRepository.findAll()).thenReturn(Collections.emptyList());

        List<ReservationAdminResponse> resultats = reservationAdminService.listerReservations();

        assertTrue(resultats.isEmpty());
    }
}