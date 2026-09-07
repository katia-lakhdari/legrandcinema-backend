package com.legrandcinema.service;

import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Salle;
import com.legrandcinema.repository.SeanceRepository;
import com.legrandcinema.repository.FilmRepository;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeanceServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private SalleRepository salleRepository;

    @InjectMocks
    private SeanceService seanceService;

    private Film film;
    private Salle salle;
    private Seance seance;
    private SeanceRequest request;

    @BeforeEach
    void setUp() {
        film = new Film();
        film.setId(1L);

        salle = new Salle();
        salle.setId(1L);

        seance = new Seance();
        seance.setId(1L);
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(LocalDateTime.now().plusDays(1));
        seance.setPrix(new BigDecimal("9.50"));

        request = new SeanceRequest();
        request.setFilmId(1L);
        request.setSalleId(1L);
        request.setDateHeure(LocalDateTime.now().plusDays(1));
        request.setPrix(new BigDecimal("9.50"));
    }

    @Test
    void listerToutesLesSeances_retourneLaListe() {
        when(seanceRepository.findAll()).thenReturn(List.of(seance));

        List<Seance> resultat = seanceService.listerToutesLesSeances();

        assertEquals(1, resultat.size());
        assertEquals(seance, resultat.get(0));
    }

    @Test
    void trouverParId_seanceExiste_retourneLaSeance() {
        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));

        Seance resultat = seanceService.trouverParId(1L);

        assertEquals(seance, resultat);
    }

    @Test
    void trouverParId_seanceInexistante_leveException() {
        when(seanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.trouverParId(99L));
    }

    @Test
    void creerSeance_casNominal_retourneLaSeanceCreee() {
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.save(any(Seance.class))).thenReturn(seance);

        Seance resultat = seanceService.creerSeance(request);

        assertEquals(new BigDecimal("9.50"), resultat.getPrix());
        verify(seanceRepository, times(1)).save(any(Seance.class));
    }

    @Test
    void creerSeance_filmInexistant_leveException() {
        when(filmRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.creerSeance(request));
        verify(seanceRepository, never()).save(any(Seance.class));
    }

    @Test
    void creerSeance_salleInexistante_leveException() {
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.creerSeance(request));
        verify(seanceRepository, never()).save(any(Seance.class));
    }

    @Test
    void modifierSeance_casNominal_retourneLaSeanceModifiee() {
        request.setPrix(new BigDecimal("12.00"));
        Seance seanceModifiee = new Seance();
        seanceModifiee.setId(1L);
        seanceModifiee.setPrix(new BigDecimal("12.00"));

        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.save(any(Seance.class))).thenReturn(seanceModifiee);

        Seance resultat = seanceService.modifierSeance(1L, request);

        assertEquals(new BigDecimal("12.00"), resultat.getPrix());
    }

    @Test
    void modifierSeance_filmInexistant_leveException() {
        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));
        when(filmRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.modifierSeance(1L, request));
        verify(seanceRepository, never()).save(any(Seance.class));
    }

    @Test
    void modifierSeance_salleInexistante_leveException() {
        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.modifierSeance(1L, request));
        verify(seanceRepository, never()).save(any(Seance.class));
    }

    @Test
    void supprimerSeance_casNominal_supprimeLaSeance() {
        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));

        seanceService.supprimerSeance(1L);

        verify(seanceRepository, times(1)).delete(seance);
    }

    @Test
    void supprimerSeance_seanceInexistante_leveException() {
        when(seanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seanceService.supprimerSeance(99L));
        verify(seanceRepository, never()).delete(any(Seance.class));
    }
}