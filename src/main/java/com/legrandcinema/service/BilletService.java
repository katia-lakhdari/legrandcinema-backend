package com.legrandcinema.service;

import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.repository.BilletRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BilletService {

    private final BilletRepository billetRepository;

    public BilletService(BilletRepository billetRepository) {
        this.billetRepository = billetRepository;
    }

    public Billet creerBillet(Reservation reservation) {
        if (billetRepository.existsByReservationId(reservation.getId())) {
            throw new RuntimeException("Un billet existe déjà pour cette réservation");
        }

        Billet billet = new Billet();
        billet.setReservation(reservation);
        billet.setQrCode(UUID.randomUUID().toString());
        billet.setScanne(false);

        return billetRepository.save(billet);
    }
}