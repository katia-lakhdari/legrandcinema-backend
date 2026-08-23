package com.legrandcinema.controller;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.dto.response.FilmResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
public class FilmController {

    @Autowired
    private FilmService filmService;

    @GetMapping
    public List<FilmResponse> listerFilms() {
        List<Film> films = filmService.listerTousLesFilms();
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

    @PostMapping
    public FilmResponse creerFilm(@RequestBody FilmRequest request) {
        Film film = filmService.creerFilm(request);
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription());
    }

    @PutMapping("/{id}")
    public FilmResponse modifierFilm(@PathVariable Long id, @RequestBody FilmRequest request) {
        Film film = filmService.modifierFilm(id, request);
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(), film.getDuree(), film.getAffiche(), film.getDescription());
    }

    @DeleteMapping("/{id}")
    public void supprimerFilm(@PathVariable Long id) {
        filmService.supprimerFilm(id);
    }
}