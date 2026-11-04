package com.legrandcinema.service;

import com.legrandcinema.dto.response.TauxRemplissageResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatistiqueServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private StatistiqueService statistiqueService;

    private Seance seance;

    @BeforeEach
    void setUp() {
        Film film = new Film();
        film.setTitre("Interstellar");

        seance = new Seance();
        seance.setId(1L);
        seance.setFilm(film);
        seance.setDateHeure(LocalDateTime.of(2026, 9, 15, 20, 0));
    }

    @Test
    void calculerTauxRemplissage_casNormal_calculeCorrectement() {
        Place reservee1 = new Place();
        reservee1.setStatut(Place.StatutPlace.RESERVEE);
        Place reservee2 = new Place();
        reservee2.setStatut(Place.StatutPlace.RESERVEE);
        Place libre = new Place();
        libre.setStatut(Place.StatutPlace.LIBRE);
        Place verrouillee = new Place();
        verrouillee.setStatut(Place.StatutPlace.VERROUILLEE);
        Place bloquee = new Place();
        bloquee.setStatut(Place.StatutPlace.BLOQUEE);

        when(seanceRepository.findAll()).thenReturn(List.of(seance));
        when(placeRepository.findBySeanceId(1L))
                .thenReturn(Arrays.asList(reservee1, reservee2, libre, verrouillee, bloquee));

        List<TauxRemplissageResponse> resultats = statistiqueService.calculerTauxRemplissage();

        TauxRemplissageResponse resultat = resultats.get(0);
        assertEquals(1, resultats.size());
        assertEquals(2, resultat.getPlacesReservees());
        assertEquals(4, resultat.getPlacesDisponibles());
        assertEquals(50.0, resultat.getTauxRemplissage());
        assertEquals("Interstellar", resultat.getFilmTitre());
    }

    @Test
    void calculerTauxRemplissage_toutesLesPlacesBloquees_renvoieZero() {
        Place bloquee1 = new Place();
        bloquee1.setStatut(Place.StatutPlace.BLOQUEE);
        Place bloquee2 = new Place();
        bloquee2.setStatut(Place.StatutPlace.BLOQUEE);

        when(seanceRepository.findAll()).thenReturn(List.of(seance));
        when(placeRepository.findBySeanceId(1L))
                .thenReturn(Arrays.asList(bloquee1, bloquee2));

        List<TauxRemplissageResponse> resultats = statistiqueService.calculerTauxRemplissage();

        assertEquals(0.0, resultats.get(0).getTauxRemplissage());
        assertEquals(0, resultats.get(0).getPlacesDisponibles());
    }

    @Test
    void calculerTauxRemplissage_seanceSansPlace_renvoieZero() {
        when(seanceRepository.findAll()).thenReturn(List.of(seance));
        when(placeRepository.findBySeanceId(1L)).thenReturn(Collections.emptyList());

        List<TauxRemplissageResponse> resultats = statistiqueService.calculerTauxRemplissage();

        assertEquals(0.0, resultats.get(0).getTauxRemplissage());
        assertEquals(0, resultats.get(0).getPlacesReservees());
    }

    @Test
    void calculerTauxRemplissage_plusieursSeances_renvoieUneEntreeParSeance() {
        Film film2 = new Film();
        film2.setTitre("Dune");
        Seance seance2 = new Seance();
        seance2.setId(2L);
        seance2.setFilm(film2);
        seance2.setDateHeure(LocalDateTime.of(2026, 9, 16, 18, 0));

        Place reservee = new Place();
        reservee.setStatut(Place.StatutPlace.RESERVEE);

        when(seanceRepository.findAll()).thenReturn(Arrays.asList(seance, seance2));
        when(placeRepository.findBySeanceId(1L)).thenReturn(List.of(reservee));
        when(placeRepository.findBySeanceId(2L)).thenReturn(Collections.emptyList());

        List<TauxRemplissageResponse> resultats = statistiqueService.calculerTauxRemplissage();

        assertEquals(2, resultats.size());
        assertEquals(1L, resultats.get(0).getSeanceId());
        assertEquals(2L, resultats.get(1).getSeanceId());
    }

    @Test
    void calculerTauxRemplissage_aucuneSeance_renvoieListeVide() {
        when(seanceRepository.findAll()).thenReturn(Collections.emptyList());

        List<TauxRemplissageResponse> resultats = statistiqueService.calculerTauxRemplissage();

        assertTrue(resultats.isEmpty());
    }
}