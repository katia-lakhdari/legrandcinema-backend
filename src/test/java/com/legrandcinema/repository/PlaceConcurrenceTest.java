package com.legrandcinema.repository;

import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.entity.Seance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlaceConcurrenceTest {

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    void verrouillerPlace_deuxClientsEnMemeTemps_leSecondEstRefuse() {
        Place place = creerPlaceLibre();

        Place copieClientA = placeRepository.findById(place.getId()).orElseThrow();
        Place copieClientB = placeRepository.findById(place.getId()).orElseThrow();

        copieClientA.setStatut(Place.StatutPlace.VERROUILLEE);
        placeRepository.save(copieClientA);

        copieClientB.setStatut(Place.StatutPlace.VERROUILLEE);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> placeRepository.save(copieClientB));
    }

    private Place creerPlaceLibre() {
        Film film = new Film();
        film.setTitre("Film de test");
        film.setGenre("Drame");
        film.setDuree(120);
        film = filmRepository.save(film);

        Salle salle = new Salle();
        salle.setNom("Salle de test");
        salle.setType(Salle.TypeSalle.STANDARD);
        salle.setCapacite(20);
        salle = salleRepository.save(salle);

        Seance seance = new Seance();
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(LocalDateTime.now().plusDays(1));
        seance.setPrix(new BigDecimal("10.00"));
        seance = seanceRepository.save(seance);

        Place place = new Place();
        place.setSeance(seance);
        place.setNumero("A1");
        place.setStatut(Place.StatutPlace.LIBRE);
        return placeRepository.save(place);
    }
}