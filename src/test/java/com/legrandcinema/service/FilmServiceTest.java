package com.legrandcinema.service;

import com.legrandcinema.entity.Film;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private FilmService filmService;

    @Test
    void listerFilmsAlAffiche_filmsAvecSeancesAVenir_retourneLesFilms() {
        Film film = new Film();
        film.setId(1L);
        film.setTitre("Dune");
        when(seanceRepository.trouverFilmsAvecSeanceAVenir(any(LocalDateTime.class))).thenReturn(List.of(film));

        List<Film> resultat = filmService.listerFilmsAlAffiche();

        assertEquals(1, resultat.size());
        assertEquals(film, resultat.get(0));
    }

    @Test
    void listerFilmsAlAffiche_aucunFilm_retourneListeVide() {
        when(seanceRepository.trouverFilmsAvecSeanceAVenir(any(LocalDateTime.class))).thenReturn(List.of());

        List<Film> resultat = filmService.listerFilmsAlAffiche();

        assertTrue(resultat.isEmpty());
    }

    @Test
    void supprimerFilm_filmSansSeance_supprimeLeFilm() {
        Film film = new Film();
        film.setId(1L);
        film.setTitre("Dune");
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(seanceRepository.existsByFilmId(1L)).thenReturn(false);

        filmService.supprimerFilm(1L);

        verify(filmRepository, times(1)).delete(film);
    }

    @Test
    void supprimerFilm_filmAvecSeances_refuseEtNeSupprimePas() {
        Film film = new Film();
        film.setId(1L);
        film.setTitre("Dune");
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(seanceRepository.existsByFilmId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> filmService.supprimerFilm(1L));

        assertEquals("Impossible de supprimer ce film : il a des séances programmées", exception.getMessage());
        verify(filmRepository, never()).delete(any(Film.class));
    }

    @Test
    void supprimerFilm_filmInexistant_leve404SansConsulterLesSeances() {
        when(filmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> filmService.supprimerFilm(99L));

        verify(seanceRepository, never()).existsByFilmId(anyLong());
        verify(filmRepository, never()).delete(any(Film.class));
    }
}