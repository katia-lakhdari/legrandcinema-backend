package com.legrandcinema.service;

import com.legrandcinema.dto.response.ReservationAdminResponse;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationAdminService {

    private final ReservationRepository reservationRepository;

    public List<ReservationAdminResponse> listerReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        List<ReservationAdminResponse> resultats = new ArrayList<>();

        for (Reservation reservation : reservations) {
            resultats.add(new ReservationAdminResponse(
                    reservation.getId(),
                    reservation.getUtilisateur().getPrenom() + " " + reservation.getUtilisateur().getNom(),
                    reservation.getSeance().getFilm().getTitre(),
                    reservation.getSeance().getDateHeure(),
                    reservation.getPlaces().size(),
                    reservation.getStatut().name()
            ));
        }

        return resultats;
    }
}