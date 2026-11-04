package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ScanBilletRequest;
import com.legrandcinema.dto.response.BilletResponse;
import com.legrandcinema.dto.response.ScanBilletResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.service.BilletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/billets")
@Tag(name = "Billets", description = "Billets, QR code et scan à l'entrée")
public class BilletController {

    private final BilletService billetService;

    public BilletController(BilletService billetService) {
        this.billetService = billetService;
    }

    @Operation(summary = "Scanner un billet à l'entrée de la salle (admin)")
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

    @Operation(summary = "Lister les billets du client connecté")
    @GetMapping("/mes-billets")
    public List<BilletResponse> mesBillets(Authentication authentication) {
        return billetService.mesBillets(authentication.getName());
    }

    @Operation(summary = "Afficher l'image QR code d'un billet")
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> obtenirQrCode(@PathVariable Long id, Authentication authentication) {
        byte[] image = billetService.obtenirImageQrCode(id, authentication.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}