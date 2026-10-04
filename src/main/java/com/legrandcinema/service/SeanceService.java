package com.legrandcinema.service;

import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final FilmRepository filmRepository;
    private final SalleRepository salleRepository;
    private final PlaceRepository placeRepository;
    private final ReservationRepository reservationRepository;

    public SeanceService(SeanceRepository seanceRepository,
                         FilmRepository filmRepository,
                         SalleRepository salleRepository,
                         PlaceRepository placeRepository,
                         ReservationRepository reservationRepository) {
        this.seanceRepository = seanceRepository;
        this.filmRepository = filmRepository;
        this.salleRepository = salleRepository;
        this.placeRepository = placeRepository;
        this.reservationRepository = reservationRepository;
    }

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

    @Transactional
    public void supprimerSeance(Long id) {
        Seance seance = trouverParId(id);

        if (reservationRepository.existsBySeanceId(id)) {
            throw new RuntimeException("Impossible de supprimer cette séance : elle a des réservations");
        }

        if (placeRepository.existsBySeanceIdAndStatutNot(id, Place.StatutPlace.LIBRE)) {
            throw new RuntimeException("Impossible de supprimer cette séance : des places sont en cours de sélection");
        }

        placeRepository.deleteAll(placeRepository.findBySeanceId(id));
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