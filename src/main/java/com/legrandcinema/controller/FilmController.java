package com.legrandcinema.controller;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.dto.response.FilmResponse;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.FilmService;
import com.legrandcinema.service.SeanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
public class FilmController {

    private final FilmService filmService;
    private final SeanceService seanceService;

    public FilmController(FilmService filmService, SeanceService seanceService) {
        this.filmService = filmService;
        this.seanceService = seanceService;
    }

    @GetMapping
    public List<FilmResponse> listerFilms() {
        return filmService.listerTousLesFilms().stream()
                .map(FilmResponse::new)
                .toList();
    }

    @GetMapping("/a-l-affiche")
    public List<FilmResponse> listerFilmsAlAffiche() {
        return filmService.listerFilmsAlAffiche().stream()
                .map(FilmResponse::new)
                .toList();
    }

    @GetMapping("/{id}")
    public FilmResponse trouverFilm(@PathVariable Long id) {
        Film film = filmService.trouverParId(id);
        return new FilmResponse(film);
    }

    @GetMapping("/{id}/seances")
    public List<SeanceResponse> listerSeancesDuFilm(@PathVariable Long id) {
        List<Seance> seances = seanceService.listerSeancesAVenirDuFilm(id);
        return seances.stream()
                .map(seance -> new SeanceResponse(seance, seanceService.compterPlacesLibres(seance.getId())))
                .toList();
    }

    @PostMapping
    public FilmResponse creerFilm(@Valid @RequestBody FilmRequest request) {
        Film film = filmService.creerFilm(request);
        return new FilmResponse(film);
    }

    @PutMapping("/{id}")
    public FilmResponse modifierFilm(@PathVariable Long id, @Valid @RequestBody FilmRequest request) {
        Film film = filmService.modifierFilm(id, request);
        return new FilmResponse(film);
    }

    @DeleteMapping("/{id}")
    public void supprimerFilm(@PathVariable Long id) {
        filmService.supprimerFilm(id);
    }
}