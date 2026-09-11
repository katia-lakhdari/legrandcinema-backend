package com.legrandcinema.controller;

import com.legrandcinema.dto.response.ReservationAdminResponse;
import com.legrandcinema.dto.response.TauxRemplissageResponse;
import com.legrandcinema.service.ReservationAdminService;
import com.legrandcinema.service.StatistiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StatistiqueService statistiqueService;
    private final ReservationAdminService reservationAdminService;

    @GetMapping("/statistiques")
    public List<TauxRemplissageResponse> getTauxRemplissage() {
        return statistiqueService.calculerTauxRemplissage();
    }

    @GetMapping("/reservations")
    public List<ReservationAdminResponse> getReservations() {
        return reservationAdminService.listerReservations();
    }
}