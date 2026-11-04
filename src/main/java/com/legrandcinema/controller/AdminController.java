package com.legrandcinema.controller;

import com.legrandcinema.dto.response.ReservationAdminResponse;
import com.legrandcinema.dto.response.TauxRemplissageResponse;
import com.legrandcinema.service.ReservationAdminService;
import com.legrandcinema.service.StatistiqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Statistiques et suivi des réservations")
public class AdminController {

    private final StatistiqueService statistiqueService;
    private final ReservationAdminService reservationAdminService;

    @Operation(summary = "Afficher le taux de remplissage des séances (admin)")
    @GetMapping("/statistiques")
    public List<TauxRemplissageResponse> getTauxRemplissage() {
        return statistiqueService.calculerTauxRemplissage();
    }

    @Operation(summary = "Lister toutes les réservations (admin)")
    @GetMapping("/reservations")
    public List<ReservationAdminResponse> getReservations() {
        return reservationAdminService.listerReservations();
    }
}