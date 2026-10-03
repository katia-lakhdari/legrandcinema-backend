package com.legrandcinema.service;

import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.entity.Place;
import com.legrandcinema.repository.SeanceRepository;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeanceService {

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private PlaceRepository placeRepository;

    public List<Seance> listerToutesLesSeances() {
        return seanceRepository.findAll();
    }

    public Seance trouverParId(Long id) {
        return seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));
    }

    public List<Seance> listerSeancesAVenirDuFilm(Long filmId) {
        if (!filmRepository.existsById(filmId)) {
            throw new ResourceNotFoundException("Film introuvable");
        }
        return seanceRepository.trouverSeancesAVenirDuFilm(filmId, LocalDateTime.now());
    }

    public long compterPlacesLibres(Long seanceId) {
        return placeRepository.countBySeanceIdAndStatut(seanceId, Place.StatutPlace.LIBRE);
    }

    public Seance creerSeance(SeanceRequest request) {
        Film film = filmRepository.findById(request.getFilmId())
                .orElseThrow(() -> new ResourceNotFoundException("Film introuvable"));
        Salle salle = salleRepository.findById(request.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));

        verifierSalleLibre(salle, film, request.getDateHeure(), null);

        Seance seance = new Seance();
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(request.getDateHeure());
        seance.setPrix(request.getPrix());
        Seance seanceSauvegardee = seanceRepository.save(seance);

        int capacite = salle.getCapacite();
        int siegesParRangee = 20;
        int nombreRangeesCompletes = capacite / siegesParRangee;
        int siegesRangeeIncomplete = capacite % siegesParRangee;

        List<Place> places = new ArrayList<>();
        char lettreRangee = 'A';

        for (int rangee = 0; rangee < nombreRangeesCompletes; rangee++) {
            for (int numeroSiege = 1; numeroSiege <= siegesParRangee; numeroSiege++) {
                Place place = new Place();
                place.setSeance(seanceSauvegardee);
                place.setNumero(lettreRangee + String.valueOf(numeroSiege));
                place.setStatut(Place.StatutPlace.LIBRE);
                places.add(place);
            }
            lettreRangee++;
        }

        if (siegesRangeeIncomplete > 0) {
            for (int numeroSiege = 1; numeroSiege <= siegesRangeeIncomplete; numeroSiege++) {
                Place place = new Place();
                place.setSeance(seanceSauvegardee);
                place.setNumero(lettreRangee + String.valueOf(numeroSiege));
                place.setStatut(Place.StatutPlace.LIBRE);
                places.add(place);
            }
        }

        placeRepository.saveAll(places);

        return seanceSauvegardee;
    }

    public Seance modifierSeance(Long id, SeanceRequest request) {
        Seance seance = trouverParId(id);
        Film film = filmRepository.findById(request.getFilmId())
                .orElseThrow(() -> new ResourceNotFoundException("Film introuvable"));
        Salle salle = salleRepository.findById(request.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable"));

        if (!seance.getSalle().getId().equals(salle.getId())) {
            throw new RuntimeException("Impossible de changer la salle d'une séance : supprimez-la et créez-en une nouvelle");
        }

        verifierSalleLibre(salle, film, request.getDateHeure(), seance.getId());

        seance.setFilm(film);
        seance.setDateHeure(request.getDateHeure());
        seance.setPrix(request.getPrix());
        return seanceRepository.save(seance);
    }

    public void supprimerSeance(Long id) {
        Seance seance = trouverParId(id);
        seanceRepository.delete(seance);
    }

    private void verifierSalleLibre(Salle salle, Film film, LocalDateTime debut, Long idSeanceAIgnorer) {
        LocalDateTime fin = debut.plusMinutes(film.getDuree());
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

        List<Seance> seancesDeLaSalle = seanceRepository.trouverSeancesDeLaSalle(salle.getId());

        for (Seance autreSeance : seancesDeLaSalle) {
            if (autreSeance.getId().equals(idSeanceAIgnorer)) {
                continue;
            }

            LocalDateTime debutAutre = autreSeance.getDateHeure();
            LocalDateTime finAutre = debutAutre.plusMinutes(autreSeance.getFilm().getDuree());

            if (debut.isBefore(finAutre) && debutAutre.isBefore(fin)) {
                throw new RuntimeException("La salle est déjà occupée du "
                        + debutAutre.format(format) + " au " + finAutre.format(format));
            }
        }
    }
}