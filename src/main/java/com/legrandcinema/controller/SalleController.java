package com.legrandcinema.controller;

import com.legrandcinema.dto.request.SalleRequest;
import com.legrandcinema.dto.response.SalleResponse;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.service.SalleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
@Tag(name = "Salles", description = "Salles du cinéma")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @Operation(summary = "Lister toutes les salles")
    @GetMapping
    public List<SalleResponse> listerSalles() {
        return salleService.listerToutesLesSalles().stream()
                .map(SalleResponse::new)
                .toList();
    }

    @Operation(summary = "Afficher le détail d'une salle")
    @GetMapping("/{id}")
    public SalleResponse trouverSalle(@PathVariable Long id) {
        Salle salle = salleService.trouverParId(id);
        return new SalleResponse(salle);
    }

    @Operation(summary = "Ajouter une salle (admin)")
    @PostMapping
    public SalleResponse creerSalle(@Valid @RequestBody SalleRequest request) {
        Salle salle = salleService.creerSalle(request);
        return new SalleResponse(salle);
    }

    @Operation(summary = "Modifier une salle (admin)")
    @PutMapping("/{id}")
    public SalleResponse modifierSalle(@PathVariable Long id, @Valid @RequestBody SalleRequest request) {
        Salle salle = salleService.modifierSalle(id, request);
        return new SalleResponse(salle);
    }

    @Operation(summary = "Supprimer une salle (admin)")
    @DeleteMapping("/{id}")
    public void supprimerSalle(@PathVariable Long id) {
        salleService.supprimerSalle(id);
    }
}