package com.legrandcinema.controller;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.dto.response.FilmResponse;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.FilmService;
import com.legrandcinema.service.SeanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
public class FilmController {

    @Autowired
    private FilmService filmService;

    @Autowired
    private SeanceService seanceService;

    @GetMapping
    public List<FilmResponse> listerFilms() {
        List<Film> films = filmService.listerTousLesFilms();
        List<FilmResponse> reponses = new java.util.ArrayList<>();
        for (Film film : films) {
            reponses.add(new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription()));
        }
        return reponses;
    }

    @GetMapping("/a-l-affiche")
    public List<FilmResponse> listerFilmsAlAffiche() {
        List<Film> films = filmService.listerFilmsAlAffiche();
        List<FilmResponse> reponses = new java.util.ArrayList<>();
        for (Film film : films) {
            reponses.add(new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription()));
        }
        return reponses;
    }

    @GetMapping("/{id}")
    public FilmResponse trouverFilm(@PathVariable Long id) {
        Film film = filmService.trouverParId(id);
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription());
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
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription());
    }

    @PutMapping("/{id}")
    public FilmResponse modifierFilm(@PathVariable Long id, @Valid @RequestBody FilmRequest request) {
        Film film = filmService.modifierFilm(id, request);
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription());
    }

    @DeleteMapping("/{id}")
    public void supprimerFilm(@PathVariable Long id) {
        filmService.supprimerFilm(id);
    }
}