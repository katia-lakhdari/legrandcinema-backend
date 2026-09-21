package com.legrandcinema.service;

import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.BilletRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BilletService {

    private final BilletRepository billetRepository;
    private final UtilisateurRepository utilisateurRepository;

    public BilletService(BilletRepository billetRepository, UtilisateurRepository utilisateurRepository) {
        this.billetRepository = billetRepository;
        this.utilisateurRepository = utilisateurRepository;
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

    public Billet scannerBillet(String qrCode) {
        Billet billet = billetRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Billet introuvable"));

        if (billet.isScanne()) {
            throw new RuntimeException("Ce billet a déjà été scanné");
        }

        billet.setScanne(true);
        return billetRepository.save(billet);
    }

    public List<Billet> mesBillets(String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return billetRepository.findByReservationUtilisateurId(utilisateur.getId());
    }
}