package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ScanBilletRequest;
import com.legrandcinema.dto.response.ScanBilletResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.service.BilletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billets")
public class BilletController {

    private final BilletService billetService;

    public BilletController(BilletService billetService) {
        this.billetService = billetService;
    }

    @PostMapping("/scan")
    public ScanBilletResponse scannerBillet(@Valid @RequestBody ScanBilletRequest requete) {
        Billet billet = billetService.scannerBillet(requete.getQrCode());
        Reservation reservation = billet.getReservation();

        return new ScanBilletResponse(
                "Billet valide, accès autorisé",
                reservation.getSeance().getFilm().getTitre(),
                reservation.getSeance().getDateHeure().toString(),
                reservation.getUtilisateur().getPrenom() + " " + reservation.getUtilisateur().getNom()
        );
    }
}