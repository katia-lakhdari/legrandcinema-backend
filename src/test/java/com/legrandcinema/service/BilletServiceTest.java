package com.legrandcinema.service;

import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.repository.BilletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BilletServiceTest {

    @Mock
    private BilletRepository billetRepository;

    @InjectMocks
    private BilletService billetService;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        reservation.setId(1L);
    }

    @Test
    void creerBillet_casNominal_creeLeBilletAvecQrCode() {
        when(billetRepository.existsByReservationId(1L)).thenReturn(false);
        when(billetRepository.save(any(Billet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Billet resultat = billetService.creerBillet(reservation);

        assertNotNull(resultat.getQrCode());
        assertFalse(resultat.getQrCode().isEmpty());
        assertFalse(resultat.isScanne());
        assertEquals(reservation, resultat.getReservation());
        verify(billetRepository, times(1)).save(any(Billet.class));
    }

    @Test
    void creerBillet_billetDejaExistant_leveException() {
        when(billetRepository.existsByReservationId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billetService.creerBillet(reservation));

        assertEquals("Un billet existe déjà pour cette réservation", exception.getMessage());
        verify(billetRepository, never()).save(any(Billet.class));
    }
}