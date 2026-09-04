package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long seanceId;
    private List<String> numerosPlaces;
    private Reservation.StatutReservation statut;
    private LocalDateTime dateReservation;
}