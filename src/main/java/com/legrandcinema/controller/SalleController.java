package com.legrandcinema.controller;

import com.legrandcinema.dto.request.SalleRequest;
import com.legrandcinema.dto.response.SalleResponse;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.service.SalleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @GetMapping
    public List<SalleResponse> listerSalles() {
        return salleService.listerToutesLesSalles().stream()
                .map(SalleResponse::new)
                .toList();
    }

    @GetMapping("/{id}")
    public SalleResponse trouverSalle(@PathVariable Long id) {
        Salle salle = salleService.trouverParId(id);
        return new SalleResponse(salle);
    }

    @PostMapping
    public SalleResponse creerSalle(@Valid @RequestBody SalleRequest request) {
        Salle salle = salleService.creerSalle(request);
        return new SalleResponse(salle);
    }

    @PutMapping("/{id}")
    public SalleResponse modifierSalle(@PathVariable Long id, @Valid @RequestBody SalleRequest request) {
        Salle salle = salleService.modifierSalle(id, request);
        return new SalleResponse(salle);
    }

    @DeleteMapping("/{id}")
    public void supprimerSalle(@PathVariable Long id) {
        salleService.supprimerSalle(id);
    }
}