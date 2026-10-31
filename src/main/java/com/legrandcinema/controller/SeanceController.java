package com.legrandcinema.controller;

import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seances")
@Tag(name = "Séances", description = "Horaires de projection")
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @Operation(summary = "Lister toutes les séances")
    @GetMapping
    public List<SeanceResponse> listerSeances() {
        return seanceService.listerToutesLesSeances().stream()
                .map(this::construireReponse)
                .toList();
    }

    @Operation(summary = "Afficher le détail d'une séance")
    @GetMapping("/{id}")
    public SeanceResponse trouverSeance(@PathVariable Long id) {
        Seance seance = seanceService.trouverParId(id);
        return construireReponse(seance);
    }

    @Operation(summary = "Programmer une séance (admin)")
    @PostMapping
    public SeanceResponse creerSeance(@Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.creerSeance(request);
        return construireReponse(seance);
    }

    @Operation(summary = "Modifier une séance (admin)")
    @PutMapping("/{id}")
    public SeanceResponse modifierSeance(@PathVariable Long id, @Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.modifierSeance(id, request);
        return construireReponse(seance);
    }

    @Operation(summary = "Supprimer une séance (admin)")
    @DeleteMapping("/{id}")
    public void supprimerSeance(@PathVariable Long id) {
        seanceService.supprimerSeance(id);
    }

    private SeanceResponse construireReponse(Seance seance) {
        long placesLibres = seanceService.compterPlacesLibres(seance.getId());
        return new SeanceResponse(seance, placesLibres);
    }
}