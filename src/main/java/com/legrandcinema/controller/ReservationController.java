package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ReservationRequest;
import com.legrandcinema.dto.response.ReservationResponse;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Création d'une réservation")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Réserver les places choisies pour une séance")
    @PostMapping
    public ReservationResponse creerReservation(@Valid @RequestBody ReservationRequest requete, Authentication authentication) {
        Reservation reservation = reservationService.creerReservation(requete, authentication.getName());

        List<String> numerosPlaces = reservation.getPlaces().stream()
                .map(Place::getNumero)
                .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeance().getId(),
                numerosPlaces,
                reservation.getStatut(),
                reservation.getDateReservation()
        );
    }
}