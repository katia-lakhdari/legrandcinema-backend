package com.legrandcinema.controller;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.dto.response.FilmResponse;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.FilmService;
import com.legrandcinema.service.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
@Tag(name = "Films", description = "Catalogue des films et leurs séances")
public class FilmController {

    private final FilmService filmService;
    private final SeanceService seanceService;

    public FilmController(FilmService filmService, SeanceService seanceService) {
        this.filmService = filmService;
        this.seanceService = seanceService;
    }

    @Operation(summary = "Lister tous les films")
    @GetMapping
    public List<FilmResponse> listerFilms() {
        return filmService.listerTousLesFilms().stream()
                .map(FilmResponse::new)
                .toList();
    }

    @Operation(summary = "Lister les films à l'affiche")
    @GetMapping("/a-l-affiche")
    public List<FilmResponse> listerFilmsAlAffiche() {
        return filmService.listerFilmsAlAffiche().stream()
                .map(FilmResponse::new)
                .toList();
    }

    @Operation(summary = "Afficher le détail d'un film")
    @GetMapping("/{id}")
    public FilmResponse trouverFilm(@PathVariable Long id) {
        Film film = filmService.trouverParId(id);
        return new FilmResponse(film);
    }

    @Operation(summary = "Lister les séances d'un film")
    @GetMapping("/{id}/seances")
    public List<SeanceResponse> listerSeancesDuFilm(@PathVariable Long id) {
        List<Seance> seances = seanceService.listerSeancesAVenirDuFilm(id);
        return seances.stream()
                .map(seance -> new SeanceResponse(seance, seanceService.compterPlacesLibres(seance.getId())))
                .toList();
    }

    @Operation(summary = "Ajouter un film (admin)")
    @PostMapping
    public FilmResponse creerFilm(@Valid @RequestBody FilmRequest request) {
        Film film = filmService.creerFilm(request);
        return new FilmResponse(film);
    }

    @Operation(summary = "Modifier un film (admin)")
    @PutMapping("/{id}")
    public FilmResponse modifierFilm(@PathVariable Long id, @Valid @RequestBody FilmRequest request) {
        Film film = filmService.modifierFilm(id, request);
        return new FilmResponse(film);
    }

    @Operation(summary = "Supprimer un film (admin)")
    @DeleteMapping("/{id}")
    public void supprimerFilm(@PathVariable Long id) {
        filmService.supprimerFilm(id);
    }
}