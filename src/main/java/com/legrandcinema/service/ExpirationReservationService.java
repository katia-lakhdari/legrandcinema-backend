package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpirationReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationReservationService.class);

    private final PlaceRepository placeRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementService paiementService;

    public ExpirationReservationService(PlaceRepository placeRepository,
                                        ReservationRepository reservationRepository,
                                        PaiementService paiementService) {
        this.placeRepository = placeRepository;
        this.reservationRepository = reservationRepository;
        this.paiementService = paiementService;
    }

    public int libererVerrousExpires() {
        List<Place> placesExpirees = placeRepository.findByStatutAndFinVerrouillageBefore(
                Place.StatutPlace.VERROUILLEE, LocalDateTime.now());

        if (placesExpirees.isEmpty()) {
            return 0;
        }

        Map<Long, Reservation> reservationsAAnnuler = new LinkedHashMap<>();

        for (Place place : placesExpirees) {
            Reservation reservation = place.getReservation();
            if (reservation != null
                    && reservation.getStatut() == Reservation.StatutReservation.EN_ATTENTE_PAIEMENT) {
                reservationsAAnnuler.putIfAbsent(reservation.getId(), reservation);
            }

            place.setStatut(Place.StatutPlace.LIBRE);
            place.setFinVerrouillage(null);
            place.setUtilisateurVerrouillage(null);
            place.setReservation(null);
            placeRepository.save(place);
        }

        for (Reservation reservation : reservationsAAnnuler.values()) {
            reservation.setStatut(Reservation.StatutReservation.ANNULEE);
            reservationRepository.save(reservation);
            paiementService.annulerOuRembourserIntention(reservation);
        }

        LOGGER.info("{} place(s) libérée(s) et {} réservation(s) annulée(s) après expiration du délai",
                placesExpirees.size(), reservationsAAnnuler.size());

        return placesExpirees.size();
    }
}