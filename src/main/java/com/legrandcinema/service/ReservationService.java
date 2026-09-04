package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.dto.request.ReservationRequest;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.SeanceRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Reservation creerReservation(ReservationRequest requete, String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Seance seance = seanceRepository.findById(requete.getSeanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        List<Place> places = new ArrayList<>();

        for (Long placeId : requete.getPlaceIds()) {
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Place introuvable"));

            if (place.getStatut() != Place.StatutPlace.VERROUILLEE) {
                throw new RuntimeException("Cette place n'est pas verrouillée");
            }

            if (place.getUtilisateurVerrouillage() == null
                    || !place.getUtilisateurVerrouillage().getId().equals(utilisateur.getId())) {
                throw new RuntimeException("Cette place est verrouillée par un autre utilisateur");
            }

            if (place.getFinVerrouillage() == null || place.getFinVerrouillage().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Le verrouillage de cette place a expiré");
            }

            places.add(place);
        }

        Reservation reservation = new Reservation();
        reservation.setUtilisateur(utilisateur);
        reservation.setSeance(seance);
        reservation.setStatut(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT);
        reservation.setDateReservation(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        for (Place place : places) {
            place.setReservation(reservation);
            place.setStatut(Place.StatutPlace.RESERVEE);
            place.setFinVerrouillage(null);
            place.setUtilisateurVerrouillage(null);
            placeRepository.save(place);
        }

        reservation.setPlaces(places);
        return reservation;
    }
}