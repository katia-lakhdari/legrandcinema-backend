package com.legrandcinema.service;

import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private BilletService billetService;

    @InjectMocks
    private PaiementService paiementService;

    private Reservation reservation;
    private Utilisateur utilisateur;
    private Seance seance;
    private PaiementRequest requete;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail("katia@legrandcinema.com");

        seance = new Seance();
        seance.setId(1L);
        seance.setPrix(new BigDecimal("10.00"));

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUtilisateur(utilisateur);
        reservation.setSeance(seance);
        reservation.setPlaces(List.of(new Place(), new Place()));
        reservation.setStatut(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT);

        requete = new PaiementRequest();
        requete.setReservationId(1L);
    }

    @Test
    void traiterPaiement_casNominal_retournePaiementConfirme() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");

        Billet billetMock = new Billet();
        billetMock.setQrCode("qr-code-test-123");
        when(billetService.creerBillet(reservation)).thenReturn(billetMock);

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(paymentIntentMock);

            PaiementResponse resultat = paiementService.traiterPaiement(requete, "katia@legrandcinema.com");

            assertEquals("PAYEE", resultat.getStatutPaiement());
            assertEquals(new BigDecimal("20.00"), resultat.getMontant());
            assertEquals("qr-code-test-123", resultat.getQrCode());
        }

        assertEquals(Reservation.StatutReservation.PAYEE, reservation.getStatut());
        verify(reservationRepository, times(1)).save(reservation);
        verify(billetService, times(1)).creerBillet(reservation);
    }

    @Test
    void traiterPaiement_reservationIntrouvable_leveResourceNotFoundException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paiementService.traiterPaiement(requete, "katia@legrandcinema.com"));

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void traiterPaiement_utilisateurIntrouvable_leveException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paiementService.traiterPaiement(requete, "inconnu@mail.com"));

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void traiterPaiement_reservationNAppartientPasAUtilisateur_leveException() {
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(2L);
        autreUtilisateur.setEmail("autre@mail.com");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("autre@mail.com")).thenReturn(Optional.of(autreUtilisateur));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.traiterPaiement(requete, "autre@mail.com"));

        assertEquals("Cette réservation ne vous appartient pas", exception.getMessage());
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void traiterPaiement_reservationDejaPayee_leveException() {
        reservation.setStatut(Reservation.StatutReservation.PAYEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.traiterPaiement(requete, "katia@legrandcinema.com"));

        assertEquals("Cette réservation est déjà payée", exception.getMessage());
    }

    @Test
    void traiterPaiement_reservationAnnulee_leveException() {
        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.traiterPaiement(requete, "katia@legrandcinema.com"));

        assertEquals("Cette réservation est annulée", exception.getMessage());
    }

    @Test
    void traiterPaiement_stripeLeveException_leveExceptionMetier() throws StripeException {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("Carte refusée");

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(stripeException);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.traiterPaiement(requete, "katia@legrandcinema.com"));

            assertTrue(exception.getMessage().contains("Carte refusée"));
        }

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void traiterPaiement_stripeStatutNonConfirme_leveException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("requires_action");

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(paymentIntentMock);

            assertThrows(RuntimeException.class,
                    () -> paiementService.traiterPaiement(requete, "katia@legrandcinema.com"));
        }

        verify(billetService, never()).creerBillet(any());
    }
}