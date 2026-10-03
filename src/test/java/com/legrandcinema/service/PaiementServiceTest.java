package com.legrandcinema.service;

import com.legrandcinema.dto.request.ConfirmationPaiementRequest;
import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.IntentionPaiementResponse;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    private static final long DELAI_MINUTES = 5L;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private BilletService billetService;

    @Mock
    private EmailService emailService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private PaiementService paiementService;

    private Reservation reservation;
    private Utilisateur utilisateur;
    private Seance seance;
    private Place place1;
    private Place place2;
    private PaiementRequest requeteIntention;
    private ConfirmationPaiementRequest requeteConfirmation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paiementService, "delaiVerrouillageMinutes", DELAI_MINUTES);

        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail("katia@legrandcinema.com");

        seance = new Seance();
        seance.setId(1L);
        seance.setPrix(new BigDecimal("10.00"));

        place1 = creerPlaceVerrouillee(100L, "A1");
        place2 = creerPlaceVerrouillee(101L, "A2");

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUtilisateur(utilisateur);
        reservation.setSeance(seance);
        reservation.setPlaces(List.of(place1, place2));
        reservation.setStatut(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT);

        requeteIntention = new PaiementRequest();
        requeteIntention.setReservationId(1L);

        requeteConfirmation = new ConfirmationPaiementRequest();
        requeteConfirmation.setReservationId(1L);
        requeteConfirmation.setPaymentIntentId("pi_test_123");
    }

    private Place creerPlaceVerrouillee(Long id, String numero) {
        Place place = new Place();
        place.setId(id);
        place.setNumero(numero);
        place.setSeance(seance);
        place.setStatut(Place.StatutPlace.VERROUILLEE);
        place.setUtilisateurVerrouillage(utilisateur);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(3));
        return place;
    }

    private void faireExpirerLesPlaces() {
        place1.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
        place2.setFinVerrouillage(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void creerIntentionPaiement_casNominal_retourneClientSecretEtEnregistreLIdentifiant() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getId()).thenReturn("pi_test_123");
        when(paymentIntentMock.getClientSecret()).thenReturn("pi_test_123_secret_abc");

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(paymentIntentMock);

            IntentionPaiementResponse resultat = paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com");

            assertEquals(1L, resultat.getReservationId());
            assertEquals("pi_test_123", resultat.getPaymentIntentId());
            assertEquals("pi_test_123_secret_abc", resultat.getClientSecret());
            assertEquals(new BigDecimal("20.00"), resultat.getMontant());
        }

        assertEquals("pi_test_123", reservation.getPaymentIntentId());
        assertEquals(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT, reservation.getStatut());
        verify(reservationRepository, times(1)).save(reservation);
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void creerIntentionPaiement_envoieMontantEtiquetteEtCleAStripe() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(paymentIntentMock);

            paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com");

            stripeMocke.verify(() -> PaymentIntent.create(
                    argThat((PaymentIntentCreateParams parametres) ->
                            Long.valueOf(2000L).equals(parametres.getAmount())
                                    && "1".equals(parametres.getMetadata().get("reservationId"))),
                    argThat((RequestOptions options) -> "intention-reservation-1".equals(options.getIdempotencyKey()))
            ));
        }
    }

    @Test
    void creerIntentionPaiement_stripeLeveException_leveExceptionMetier() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("Clé API invalide");

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(stripeException);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

            assertTrue(exception.getMessage().contains("La préparation du paiement a échoué"));
            assertTrue(exception.getMessage().contains("Clé API invalide"));
        }

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerIntentionPaiement_reservationIntrouvable_leveResourceNotFoundException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

        assertEquals("Réservation introuvable", exception.getMessage());
    }

    @Test
    void creerIntentionPaiement_utilisateurIntrouvable_leveException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.creerIntentionPaiement(requeteIntention, "inconnu@mail.com"));

        assertEquals("Utilisateur introuvable", exception.getMessage());
    }

    @Test
    void creerIntentionPaiement_reservationNAppartientPasAUtilisateur_leveResourceNotFound() {
        Utilisateur autreUtilisateur = new Utilisateur();
        autreUtilisateur.setId(2L);
        autreUtilisateur.setEmail("autre@mail.com");

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("autre@mail.com")).thenReturn(Optional.of(autreUtilisateur));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paiementService.creerIntentionPaiement(requeteIntention, "autre@mail.com"));

        assertEquals("Réservation introuvable", exception.getMessage());
    }

    @Test
    void creerIntentionPaiement_reservationDejaPayee_leveException() {
        reservation.setStatut(Reservation.StatutReservation.PAYEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

        assertEquals("Cette réservation est déjà payée", exception.getMessage());
    }

    @Test
    void creerIntentionPaiement_reservationAnnulee_leveException() {
        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

        assertEquals("Cette réservation est annulée", exception.getMessage());
    }

    @Test
    void creerIntentionPaiement_delaiDepasse_refuseSansAppelerStripe() {
        faireExpirerLesPlaces();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

            assertEquals("Le délai de paiement est dépassé, veuillez refaire votre réservation", exception.getMessage());
            stripeMocke.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)), never());
        }

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void creerIntentionPaiement_placeVerrouilleeParAutreClient_refuse() {
        Utilisateur autreClient = new Utilisateur();
        autreClient.setId(2L);
        place2.setUtilisateurVerrouillage(autreClient);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.creerIntentionPaiement(requeteIntention, "katia@legrandcinema.com"));

            assertEquals("Le délai de paiement est dépassé, veuillez refaire votre réservation", exception.getMessage());
            stripeMocke.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)), never());
        }
    }

    @Test
    void confirmerPaiement_casNominal_retournePaiementConfirmeEtReserveLesPlaces() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getAmount()).thenReturn(2000L);
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        Billet billetMock = new Billet();
        billetMock.setQrCode("qr-code-test-123");
        when(billetService.creerBillet(reservation)).thenReturn(billetMock);

        byte[] fausseImage = {1, 2, 3};
        when(qrCodeService.genererQrCode("qr-code-test-123")).thenReturn(fausseImage);

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            PaiementResponse resultat = paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com");

            assertEquals("PAYEE", resultat.getStatutPaiement());
            assertEquals(new BigDecimal("20.00"), resultat.getMontant());
            assertEquals("qr-code-test-123", resultat.getQrCode());
        }

        assertEquals(Reservation.StatutReservation.PAYEE, reservation.getStatut());
        for (Place place : List.of(place1, place2)) {
            assertEquals(Place.StatutPlace.RESERVEE, place.getStatut());
            assertNull(place.getFinVerrouillage());
            assertNull(place.getUtilisateurVerrouillage());
        }
        verify(placeRepository, times(2)).save(any(Place.class));
        verify(reservationRepository, times(1)).save(reservation);
        verify(billetService, times(1)).creerBillet(reservation);
        verify(emailService, times(1)).envoyerEmailAvecImage(
                eq("katia@legrandcinema.com"), anyString(), anyString(), eq(fausseImage), eq("billet-qrcode.png"));
    }

    @Test
    void confirmerPaiement_carteRefusee_prolongeLeDelaiDeCinqMinutes() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("requires_payment_method");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        LocalDateTime avant = LocalDateTime.now();

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Paiement refusé, veuillez réessayer", exception.getMessage());
        }

        LocalDateTime apres = LocalDateTime.now();

        for (Place place : List.of(place1, place2)) {
            assertEquals(Place.StatutPlace.VERROUILLEE, place.getStatut());
            assertEquals(utilisateur, place.getUtilisateurVerrouillage());
            assertFalse(place.getFinVerrouillage().isBefore(avant.plusMinutes(DELAI_MINUTES)));
            assertFalse(place.getFinVerrouillage().isAfter(apres.plusMinutes(DELAI_MINUTES)));
        }
        assertEquals(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT, reservation.getStatut());
        verify(placeRepository, times(2)).save(any(Place.class));
        verify(reservationRepository, never()).save(any());
        verify(billetService, never()).creerBillet(any());
        verify(emailService, never()).envoyerEmailAvecImage(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void confirmerPaiement_authentificationEnAttente_refuseSansProlongerLeDelai() {
        LocalDateTime finAvant = place1.getFinVerrouillage();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("requires_action");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Le paiement n'a pas été confirmé par Stripe", exception.getMessage());
        }

        assertEquals(finAvant, place1.getFinVerrouillage());
        verify(placeRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_montantDifferent_leveExceptionSansBillet() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getAmount()).thenReturn(1000L);
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Le montant payé ne correspond pas au prix de la réservation", exception.getMessage());
        }

        verify(placeRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_paiementDUneAutreReservation_leveExceptionSansBillet() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "99"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> remboursementMocke = mockStatic(Refund.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Ce paiement ne correspond pas à cette réservation", exception.getMessage());
            remboursementMocke.verify(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)), never());
        }

        verify(reservationRepository, never()).save(any());
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_stripeInjoignable_leveExceptionMetier() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("Délai dépassé");

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenThrow(stripeException);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertTrue(exception.getMessage().contains("Impossible de vérifier le paiement"));
            assertTrue(exception.getMessage().contains("Délai dépassé"));
        }

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_reservationDejaPayee_refuseSansAppelerStripe() {
        reservation.setStatut(Reservation.StatutReservation.PAYEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Cette réservation est déjà payée", exception.getMessage());
            stripeMocke.verify(() -> PaymentIntent.retrieve(anyString()), never());
        }

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_payeApresExpiration_rembourseEtRefuse() {
        faireExpirerLesPlaces();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getId()).thenReturn("pi_test_123");
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> remboursementMocke = mockStatic(Refund.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);
            remboursementMocke.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mock(Refund.class));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Le délai de paiement est dépassé : votre paiement a été remboursé", exception.getMessage());
            remboursementMocke.verify(() -> Refund.create(
                    argThat((RefundCreateParams parametres) -> "pi_test_123".equals(parametres.getPaymentIntent())),
                    argThat((RequestOptions options) -> "remboursement-pi_test_123".equals(options.getIdempotencyKey()))
            ));
        }

        assertEquals(Reservation.StatutReservation.EN_ATTENTE_PAIEMENT, reservation.getStatut());
        verify(placeRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_reservationAnnuleeMaisPayee_rembourseEtRefuse() {
        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getId()).thenReturn("pi_test_123");
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> remboursementMocke = mockStatic(Refund.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);
            remboursementMocke.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(mock(Refund.class));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Le délai de paiement est dépassé : votre paiement a été remboursé", exception.getMessage());
            remboursementMocke.verify(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)), times(1));
        }

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_remboursementEchoue_demandeDeContacterLeCinema() {
        faireExpirerLesPlaces();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getId()).thenReturn("pi_test_123");
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> remboursementMocke = mockStatic(Refund.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);
            remboursementMocke.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(new ApiConnectionException("Stripe injoignable"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertTrue(exception.getMessage().contains("le remboursement automatique a échoué"));
            assertTrue(exception.getMessage().contains("contactez le cinéma"));
        }

        verify(billetService, never()).creerBillet(any());
    }

    @Test
    void confirmerPaiement_delaiDepasseEtCarteRefusee_refuseSansRembourserNiProlonger() {
        faireExpirerLesPlaces();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("requires_payment_method");
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> remboursementMocke = mockStatic(Refund.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com"));

            assertEquals("Le délai de paiement est dépassé, veuillez refaire votre réservation", exception.getMessage());
            remboursementMocke.verify(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)), never());
        }

        verify(placeRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmerPaiement_echecEnvoiEmail_nEmpechePasLePaiement() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getAmount()).thenReturn(2000L);
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        Billet billetMock = new Billet();
        billetMock.setQrCode("qr-code-test-123");
        when(billetService.creerBillet(reservation)).thenReturn(billetMock);

        doThrow(new RuntimeException("Email indisponible"))
                .when(emailService).envoyerEmailAvecImage(anyString(), anyString(), anyString(), any(), anyString());

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            PaiementResponse resultat = paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com");

            assertEquals("PAYEE", resultat.getStatutPaiement());
            assertEquals("qr-code-test-123", resultat.getQrCode());
        }

        assertEquals(Reservation.StatutReservation.PAYEE, reservation.getStatut());
        verify(billetService, times(1)).creerBillet(reservation);
    }

    @Test
    void confirmerPaiement_echecGenerationQrCode_nEmpechePasLePaiement() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(utilisateurRepository.findByEmail("katia@legrandcinema.com")).thenReturn(Optional.of(utilisateur));

        PaymentIntent paymentIntentMock = mock(PaymentIntent.class);
        when(paymentIntentMock.getStatus()).thenReturn("succeeded");
        when(paymentIntentMock.getAmount()).thenReturn(2000L);
        when(paymentIntentMock.getMetadata()).thenReturn(Map.of("reservationId", "1"));

        Billet billetMock = new Billet();
        billetMock.setQrCode("qr-code-test-123");
        when(billetService.creerBillet(reservation)).thenReturn(billetMock);

        when(qrCodeService.genererQrCode("qr-code-test-123"))
                .thenThrow(new RuntimeException("Erreur lors de la génération du QR code"));

        try (MockedStatic<PaymentIntent> stripeMocke = mockStatic(PaymentIntent.class)) {
            stripeMocke.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntentMock);

            PaiementResponse resultat = paiementService.confirmerPaiement(requeteConfirmation, "katia@legrandcinema.com");

            assertEquals("PAYEE", resultat.getStatutPaiement());
            assertEquals("qr-code-test-123", resultat.getQrCode());
        }

        assertEquals(Reservation.StatutReservation.PAYEE, reservation.getStatut());
        verify(emailService, never()).envoyerEmailAvecImage(anyString(), anyString(), anyString(), any(), anyString());
    }
}