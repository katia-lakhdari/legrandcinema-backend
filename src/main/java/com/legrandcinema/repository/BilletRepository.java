package com.legrandcinema.repository;

import com.legrandcinema.entity.Billet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BilletRepository extends JpaRepository<Billet, Long> {
    boolean existsByReservationId(Long reservationId);
    Optional<Billet> findByQrCode(String qrCode);
}