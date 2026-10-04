package com.legrandcinema.service;

import com.legrandcinema.entity.Salle;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.SalleRepository;
import com.legrandcinema.repository.SeanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalleServiceTest {

    @Mock
    private SalleRepository salleRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private SalleService salleService;

    @Test
    void supprimerSalle_salleSansSeance_supprimeLaSalle() {
        Salle salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle 1");
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.existsBySalleId(1L)).thenReturn(false);

        salleService.supprimerSalle(1L);

        verify(salleRepository, times(1)).delete(salle);
    }

    @Test
    void supprimerSalle_salleAvecSeances_refuseEtNeSupprimePas() {
        Salle salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle 1");
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(seanceRepository.existsBySalleId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> salleService.supprimerSalle(1L));

        assertEquals("Impossible de supprimer cette salle : elle a des séances programmées", exception.getMessage());
        verify(salleRepository, never()).delete(any(Salle.class));
    }

    @Test
    void supprimerSalle_salleInexistante_leve404SansConsulterLesSeances() {
        when(salleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> salleService.supprimerSalle(99L));

        verify(seanceRepository, never()).existsBySalleId(anyLong());
        verify(salleRepository, never()).delete(any(Salle.class));
    }
}