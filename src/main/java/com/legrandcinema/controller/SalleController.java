package com.legrandcinema.controller;

import com.legrandcinema.entity.Salle;
import com.legrandcinema.service.SalleService;
import com.legrandcinema.dto.request.SalleRequest;
import com.legrandcinema.dto.response.SalleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/salles")
public class SalleController {

    @Autowired
    private SalleService salleService;

    @GetMapping
    public List<SalleResponse> listerSalles() {
        List<Salle> salles = salleService.listerToutesLesSalles();
        return salles.stream()
                .map(salle -> new SalleResponse(salle.getId(), salle.getNom(),
                        salle.getType().name(), salle.getCapacite()))
                .toList();
    }

    @GetMapping("/{id}")
    public SalleResponse trouverSalle(@PathVariable Long id) {
        Salle salle = salleService.trouverParId(id);
        return new SalleResponse(salle.getId(), salle.getNom(),
                salle.getType().name(), salle.getCapacite());
    }

    @PostMapping
    public SalleResponse creerSalle(@RequestBody SalleRequest request) {
        Salle salle = salleService.creerSalle(request);
        return new SalleResponse(salle.getId(), salle.getNom(),
                salle.getType().name(), salle.getCapacite());
    }

    @PutMapping("/{id}")
    public SalleResponse modifierSalle(@PathVariable Long id, @RequestBody SalleRequest request) {
        Salle salle = salleService.modifierSalle(id, request);
        return new SalleResponse(salle.getId(), salle.getNom(),
                salle.getType().name(), salle.getCapacite());
    }

    @DeleteMapping("/{id}")
    public void supprimerSalle(@PathVariable Long id) {
        salleService.supprimerSalle(id);
    }
}