package com.legrandcinema.controller;

import com.legrandcinema.entity.Place;
import com.legrandcinema.dto.response.PlaceResponse;
import com.legrandcinema.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    @Autowired
    private PlaceService placeService;

    @GetMapping("/seance/{seanceId}")
    public List<PlaceResponse> listerPlacesParSeance(@PathVariable Long seanceId) {
        List<Place> places = placeService.listerPlacesParSeance(seanceId);
        List<PlaceResponse> reponses = new ArrayList<>();
        for (Place place : places) {
            reponses.add(new PlaceResponse(place.getId(), place.getNumero(),
                    place.getStatut().name(), place.getFinVerrouillage()));
        }
        return reponses;
    }

    @PostMapping("/{id}/verrouiller")
    public PlaceResponse verrouillerPlace(@PathVariable Long id, Authentication authentication) {
        Place place = placeService.verrouillerPlace(id, authentication.getName());
        return new PlaceResponse(place.getId(), place.getNumero(),
                place.getStatut().name(), place.getFinVerrouillage());
    }

    @PostMapping("/{id}/liberer")
    public PlaceResponse libererPlace(@PathVariable Long id) {
        Place place = placeService.libererPlace(id);
        return new PlaceResponse(place.getId(), place.getNumero(),
                place.getStatut().name(), place.getFinVerrouillage());
    }
}