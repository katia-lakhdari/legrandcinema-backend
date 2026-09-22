package com.legrandcinema.repository;

import com.legrandcinema.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findBySeance_DateHeureAfterAndStatutNot(LocalDateTime maintenant, Reservation.StatutReservation statut);
}