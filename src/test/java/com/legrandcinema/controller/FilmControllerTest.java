package com.legrandcinema.controller;

import com.legrandcinema.dto.request.FilmRequest;
import com.legrandcinema.dto.response.FilmResponse;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.FilmService;
import com.legrandcinema.service.SeanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmControllerTest {

    @Mock
    private FilmService filmService;

    @Mock
    private SeanceService seanceService;

    @InjectMocks
    private FilmController filmController;

    private Film creerFilmDeTest(Long id, String titre) {
        Film film = new Film();
        film.setId(id);
        film.setTitre(titre);
        film.setGenre("Science-fiction");
        film.setDuree(155);
        film.setAffiche("dune.jpg");
        film.setDescription("Paul Atréides arrive sur Arrakis.");
        return film;
    }

    @Test
    void listerFilms_deuxFilms_retourneDeuxFilmResponse() {
        when(filmService.listerTousLesFilms()).thenReturn(List.of(creerFilmDeTest(1L, "Dune"), creerFilmDeTest(2L, "Whiplash")));

        List<FilmResponse> resultat = filmController.listerFilms();

        assertEquals(2, resultat.size());
        FilmResponse premierFilm = resultat.get(0);
        assertEquals(1L, premierFilm.getId());
        assertEquals("Dune", premierFilm.getTitre());
        assertEquals("Science-fiction", premierFilm.getGenre());
        assertEquals(155, premierFilm.getDuree());
        assertEquals("dune.jpg", premierFilm.getAffiche());
        assertEquals("Paul Atréides arrive sur Arrakis.", premierFilm.getDescription());
        assertEquals("Whiplash", resultat.get(1).getTitre());
    }

    @Test
    void listerFilmsAlAffiche_unFilm_retourneLeFilm() {
        when(filmService.listerFilmsAlAffiche()).thenReturn(List.of(creerFilmDeTest(1L, "Dune")));

        List<FilmResponse> resultat = filmController.listerFilmsAlAffiche();

        assertEquals(1, resultat.size());
        assertEquals("Dune", resultat.get(0).getTitre());
    }

    @Test
    void trouverFilm_idExistant_retourneLeFilm() {
        when(filmService.trouverParId(1L)).thenReturn(creerFilmDeTest(1L, "Dune"));

        FilmResponse resultat = filmController.trouverFilm(1L);

        assertEquals(1L, resultat.getId());
        assertEquals("Dune", resultat.getTitre());
    }

    @Test
    void listerSeancesDuFilm_uneSeance_retourneLaSeanceAvecSesPlacesLibres() {
        Salle salle = new Salle();
        salle.setId(3L);
        salle.setNom("Salle 1");

        Seance seance = new Seance();
        seance.setId(12L);
        seance.setFilm(creerFilmDeTest(1L, "Whiplash"));
        seance.setSalle(salle);
        seance.setDateHeure(LocalDateTime.of(2026, 11, 2, 20, 30));
        seance.setPrix(new BigDecimal("9.50"));

        when(seanceService.listerSeancesAVenirDuFilm(1L)).thenReturn(List.of(seance));
        when(seanceService.compterPlacesLibres(12L)).thenReturn(40L);

        List<SeanceResponse> resultat = filmController.listerSeancesDuFilm(1L);

        assertEquals(1, resultat.size());
        SeanceResponse seanceRecue = resultat.get(0);
        assertEquals(12L, seanceRecue.getId());
        assertEquals("Whiplash", seanceRecue.getTitreFilm());
        assertEquals("Salle 1", seanceRecue.getNomSalle());
        assertEquals(40L, seanceRecue.getPlacesLibres());
        assertFalse(seanceRecue.isComplete());
    }

    @Test
    void creerFilm_requeteValide_retourneLeFilmCree() {
        FilmRequest requete = new FilmRequest();
        requete.setTitre("Dune");
        when(filmService.creerFilm(requete)).thenReturn(creerFilmDeTest(1L, "Dune"));

        FilmResponse resultat = filmController.creerFilm(requete);

        assertEquals(1L, resultat.getId());
        assertEquals("Dune", resultat.getTitre());
    }

    @Test
    void modifierFilm_requeteValide_retourneLeFilmModifie() {
        FilmRequest requete = new FilmRequest();
        requete.setTitre("Dune : deuxième partie");
        when(filmService.modifierFilm(1L, requete)).thenReturn(creerFilmDeTest(1L, "Dune : deuxième partie"));

        FilmResponse resultat = filmController.modifierFilm(1L, requete);

        assertEquals("Dune : deuxième partie", resultat.getTitre());
    }

    @Test
    void supprimerFilm_idExistant_demandeLaSuppressionAuService() {
        filmController.supprimerFilm(1L);

        verify(filmService, times(1)).supprimerFilm(1L);
    }
}
