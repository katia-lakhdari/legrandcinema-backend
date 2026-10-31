package com.legrandcinema.controller;

import com.legrandcinema.entity.Place;
import com.legrandcinema.dto.request.BlocagePlaceRequest;
import com.legrandcinema.dto.response.PlaceResponse;
import com.legrandcinema.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@Tag(name = "Places", description = "Choix, verrouillage et blocage des places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @Operation(summary = "Lister les places d'une séance")
    @GetMapping("/seance/{seanceId}")
    public List<PlaceResponse> listerPlacesParSeance(@PathVariable Long seanceId) {
        return placeService.listerPlacesParSeance(seanceId).stream()
                .map(PlaceResponse::new)
                .toList();
    }

    @Operation(summary = "Réserver temporairement une place pendant le choix")
    @PostMapping("/{id}/verrouiller")
    public PlaceResponse verrouillerPlace(@PathVariable Long id, Authentication authentication) {
        Place place = placeService.verrouillerPlace(id, authentication.getName());
        return new PlaceResponse(place);
    }

    @Operation(summary = "Libérer une place choisie")
    @PostMapping("/{id}/liberer")
    public PlaceResponse libererPlace(@PathVariable Long id, Authentication authentication) {
        Place place = placeService.libererPlace(id, authentication.getName());
        return new PlaceResponse(place);
    }

    @Operation(summary = "Bloquer une place avec une raison (admin)")
    @PostMapping("/{id}/bloquer")
    public PlaceResponse bloquerPlace(@PathVariable Long id, @Valid @RequestBody BlocagePlaceRequest request) {
        Place place = placeService.bloquerPlace(id, request.getRaison());
        return new PlaceResponse(place);
    }

    @Operation(summary = "Débloquer une place (admin)")
    @PostMapping("/{id}/debloquer")
    public PlaceResponse debloquerPlace(@PathVariable Long id) {
        Place place = placeService.debloquerPlace(id);
        return new PlaceResponse(place);
    }
}