package com.legrandcinema.controller;

import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.SeanceService;
import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.dto.response.SeanceResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seances")
public class SeanceController {

    @Autowired
    private SeanceService seanceService;

    @GetMapping
    public List<SeanceResponse> listerSeances() {
        List<Seance> seances = seanceService.listerToutesLesSeances();
        return seances.stream()
                .map(seance -> new SeanceResponse(seance.getId(), seance.getFilm().getTitre(),
                        seance.getSalle().getNom(), seance.getDateHeure()))
                .toList();
    }

    @GetMapping("/{id}")
    public SeanceResponse trouverSeance(@PathVariable Long id) {
        Seance seance = seanceService.trouverParId(id);
        return new SeanceResponse(seance.getId(), seance.getFilm().getTitre(),
                seance.getSalle().getNom(), seance.getDateHeure());
    }

    @PostMapping
    public SeanceResponse creerSeance(@Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.creerSeance(request);
        return new SeanceResponse(seance.getId(), seance.getFilm().getTitre(),
                seance.getSalle().getNom(), seance.getDateHeure());
    }

    @PutMapping("/{id}")
    public SeanceResponse modifierSeance(@PathVariable Long id, @Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.modifierSeance(id, request);
        return new SeanceResponse(seance.getId(), seance.getFilm().getTitre(),
                seance.getSalle().getNom(), seance.getDateHeure());
    }

    @DeleteMapping("/{id}")
    public void supprimerSeance(@PathVariable Long id) {
        seanceService.supprimerSeance(id);
    }
}