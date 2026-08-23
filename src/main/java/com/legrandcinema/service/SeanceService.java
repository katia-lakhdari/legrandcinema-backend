package com.legrandcinema.service;

import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.repository.SeanceRepository;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SeanceService {

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    public List<Seance> listerToutesLesSeances() {
        return seanceRepository.findAll();
    }

    public Seance trouverParId(Long id) {
        return seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));
    }

    public Seance creerSeance(SeanceRequest request) {
        Film film = filmRepository.findById(request.getFilmId())
                .orElseThrow(() -> new ResourceNotFoundException("Film introuvable"));
        Salle salle = salleRepository.findById(request.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));

        Seance seance = new Seance();
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(request.getDateHeure());
        return seanceRepository.save(seance);
    }

    public Seance modifierSeance(Long id, SeanceRequest request) {
        Seance seance = trouverParId(id);
        Film film = filmRepository.findById(request.getFilmId())
                .orElseThrow(() -> new ResourceNotFoundException("Film introuvable"));
        Salle salle = salleRepository.findById(request.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));

        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(request.getDateHeure());
        return seanceRepository.save(seance);
    }

    public void supprimerSeance(Long id) {
        Seance seance = trouverParId(id);
        seanceRepository.delete(seance);
    }
}