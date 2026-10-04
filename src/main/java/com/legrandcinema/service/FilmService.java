package com.legrandcinema.service;

import com.legrandcinema.entity.Film;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.dto.request.FilmRequest;

@Service
public class FilmService {

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    public List<Film> listerTousLesFilms() {
        return filmRepository.findAll();
    }

    public List<Film> listerFilmsAlAffiche() {
        return seanceRepository.trouverFilmsAvecSeanceAVenir(LocalDateTime.now());
    }

    public Film trouverParId(Long id) {
        return filmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Film introuvable"));
    }

    public Film creerFilm(FilmRequest request) {
        Film film = new Film();
        film.setTitre(request.getTitre());
        film.setGenre(request.getGenre());
        film.setDuree(request.getDuree());
        film.setAffiche(request.getAffiche());
        film.setDescription(request.getDescription());
        return filmRepository.save(film);
    }

    public Film modifierFilm(Long id, FilmRequest request) {
        Film film = trouverParId(id);
        film.setTitre(request.getTitre());
        film.setGenre(request.getGenre());
        film.setDuree(request.getDuree());
        film.setAffiche(request.getAffiche());
        film.setDescription(request.getDescription());
        return filmRepository.save(film);
    }

    public void supprimerFilm(Long id) {
        Film film = trouverParId(id);

        if (seanceRepository.existsByFilmId(id)) {
            throw new RuntimeException("Impossible de supprimer ce film : il a des séances programmées");
        }

        filmRepository.delete(film);
    }
}