package com.legrandcinema.service;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.entity.Film;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FilmService {

    private final FilmRepository filmRepository;
    private final SeanceRepository seanceRepository;

    public FilmService(FilmRepository filmRepository, SeanceRepository seanceRepository) {
        this.filmRepository = filmRepository;
        this.seanceRepository = seanceRepository;
    }

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