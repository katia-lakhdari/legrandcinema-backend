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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeanceServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private SalleRepository salleRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private SeanceService seanceService;

    private Film film;
    private Salle salle;
    private Seance seance;
    private Seance autreSeance;
    private SeanceRequest request;

    @BeforeEach
    void setUp() {
        film = new Film();
        film.setId(1L);
        film.setDuree(120);

        salle = new Salle();
        salle.setId(1L);
        salle.setCapacite(70);

        seance = new Seance();
        seance.setId(1L);
        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDateHeure(LocalDateTime.now().plusDays(1));
        seance.setPrix(new BigDecimal("9.50"));

        autreSeance = new Seance();
        autreSeance.setId(2L);
        autreSeance.setFilm(film);
        autreSeance.setSalle(salle);
        autreSeance.setDateHeure(LocalDateTime.of(2026, 12, 15, 18, 0));
        autreSeance.setPrix(new BigDecimal("9.50"));

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
    void listerSeancesAVenirDuFilm_filmExiste_retourneLesSeances() {
        when(filmRepository.existsById(1L)).thenReturn(true);
        when(seanceRepository.trouverSeancesAVenirDuFilm(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(seance));

        List<Seance> resultat = seanceService.listerSeancesAVenirDuFilm(1L);

        assertEquals(1, resultat.size());
        assertEquals(seance, resultat.get(0));
    }

    @Test
    void listerSeancesAVenirDuFilm_aucuneSeance_retourneListeVide() {
        when(filmRepository.existsById(1L)).thenReturn(true);
        when(seanceRepository.trouverSeancesAVenirDuFilm(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());

        List<Seance> resultat = seanceService.listerSeancesAVenirDuFilm(1L);

        assertTrue(resultat.isEmpty());
    }

    @Test
    void listerSeancesAVenirDuFilm_filmInexistant_leveException() {
        when(filmRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> seanceService.listerSeancesAVenirDuFilm(99L));
        verify(seanceRepository, never()).trouverSeancesAVenirDuFilm(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void compterPlacesLibres_retourneLeNombreDePlacesLibres() {
        when(placeRepository.countBySeanceIdAndStatut(1L, Place.StatutPlace.LIBRE)).thenReturn(70L);

        long resultat = seanceService.compterPlacesLibres(1L);

        assertEquals(70L, resultat);
    }

    @Test
    void creerSeance_casNominal_retourneLaSeanceCreee() {
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.save(any(Seance.class))).thenReturn(seance);

        Seance resultat = seanceService.creerSeance(request);

        assertEquals(new BigDecimal("9.50"), resultat.getPrix());
        verify(seanceRepository, times(1)).save(any(Seance.class));
        verify(placeRepository, times(1)).saveAll(anyList());
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
    void creerSeance_pendantUneAutreSeance_leveException() {
        request.setDateHeure(LocalDateTime.of(2026, 12, 15, 19, 0));

        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.trouverSeancesDeLaSalle(1L)).thenReturn(List.of(autreSeance));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seanceService.creerSeance(request));

        assertEquals("La salle est déjà occupée du 15/12/2026 à 18:00 au 15/12/2026 à 20:00", exception.getMessage());
        verify(seanceRepository, never()).save(any(Seance.class));
        verify(placeRepository, never()).saveAll(anyList());
    }

    @Test
    void creerSeance_finitPendantUneAutreSeance_leveException() {
        request.setDateHeure(LocalDateTime.of(2026, 12, 15, 17, 0));

        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.trouverSeancesDeLaSalle(1L)).thenReturn(List.of(autreSeance));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seanceService.creerSeance(request));

        assertEquals("La salle est déjà occupée du 15/12/2026 à 18:00 au 15/12/2026 à 20:00", exception.getMessage());
        verify(seanceRepository, never()).save(any(Seance.class));
    }

    @Test
    void creerSeance_commenceQuandUneAutreFinit_estAcceptee() {
        request.setDateHeure(LocalDateTime.of(2026, 12, 15, 20, 0));

        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.trouverSeancesDeLaSalle(1L)).thenReturn(List.of(autreSeance));
        when(seanceRepository.save(any(Seance.class))).thenReturn(seance);

        seanceService.creerSeance(request);

        verify(seanceRepository, times(1)).save(any(Seance.class));
        verify(placeRepository, times(1)).saveAll(anyList());
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
    void modifierSeance_neSeComparePasAvecElleMeme_estAcceptee() {
        request.setDateHeure(seance.getDateHeure());
        request.setPrix(new BigDecimal("12.00"));

        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.trouverSeancesDeLaSalle(1L)).thenReturn(List.of(seance));
        when(seanceRepository.save(any(Seance.class))).thenReturn(seance);

        seanceService.modifierSeance(1L, request);

        verify(seanceRepository, times(1)).save(seance);
        assertEquals(new BigDecimal("12.00"), seance.getPrix());
    }

    @Test
    void modifierSeance_changementDeSalle_leveException() {
        Salle autreSalle = new Salle();
        autreSalle.setId(2L);
        request.setSalleId(2L);

        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seance));
        when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        when(salleRepository.findById(2L)).thenReturn(Optional.of(autreSalle));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seanceService.modifierSeance(1L, request));

        assertEquals("Impossible de changer la salle d'une séance : supprimez-la et créez-en une nouvelle", exception.getMessage());
        verify(seanceRepository, never()).save(any(Seance.class));
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