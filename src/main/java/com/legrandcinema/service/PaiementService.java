package com.legrandcinema.service;

import com.legrandcinema.dto.request.ConfirmationPaiementRequest;
import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.IntentionPaiementResponse;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaiementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaiementService.class);

    private static final String STATUT_STRIPE_REUSSI = "succeeded";
    private static final String STATUT_STRIPE_REFUSE = "requires_payment_method";

    private final ReservationRepository reservationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PlaceRepository placeRepository;
    private final BilletService billetService;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    @Value("${reservation.delai-verrouillage-minutes}")
    private long delaiVerrouillageMinutes;

    public PaiementService(ReservationRepository reservationRepository,
                           UtilisateurRepository utilisateurRepository,
                           PlaceRepository placeRepository,
                           BilletService billetService,
                           EmailService emailService,
                           QrCodeService qrCodeService) {
        this.reservationRepository = reservationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.placeRepository = placeRepository;
        this.billetService = billetService;
        this.emailService = emailService;
        this.qrCodeService = qrCodeService;
    }

    public IntentionPaiementResponse creerIntentionPaiement(PaiementRequest requete, String emailUtilisateur) {
        Reservation reservation = trouverReservationDuClient(requete.getReservationId(), emailUtilisateur);
        verifierReservationPayable(reservation);

        BigDecimal montantEnEuros = calculerMontantEnEuros(reservation);
        long montantEnCentimes = montantEnEuros.multiply(BigDecimal.valueOf(100)).longValueExact();

        PaymentIntent paymentIntent;
        try {
            PaymentIntentCreateParams parametres = PaymentIntentCreateParams.builder()
                    .setAmount(montantEnCentimes)
                    .setCurrency("eur")
                    .addPaymentMethodType("card")
                    .putMetadata("reservationId", reservation.getId().toString())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("intention-reservation-" + reservation.getId())
                    .build();

            paymentIntent = PaymentIntent.create(parametres, options);
        } catch (StripeException e) {
            throw new RuntimeException("La préparation du paiement a échoué : " + e.getMessage());
        }

        reservation.setPaymentIntentId(paymentIntent.getId());
        reservationRepository.save(reservation);

        return new IntentionPaiementResponse(
                reservation.getId(),
                paymentIntent.getId(),
                paymentIntent.getClientSecret(),
                montantEnEuros
        );
    }

    public PaiementResponse confirmerPaiement(ConfirmationPaiementRequest requete, String emailUtilisateur) {
        Reservation reservation = trouverReservationDuClient(requete.getReservationId(), emailUtilisateur);

        if (reservation.getStatut() == Reservation.StatutReservation.PAYEE) {
            throw new RuntimeException("Cette réservation est déjà payée");
        }

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(requete.getPaymentIntentId());
        } catch (StripeException e) {
            throw new RuntimeException("Impossible de vérifier le paiement auprès de Stripe : " + e.getMessage());
        }

        if (!reservation.getId().toString().equals(paymentIntent.getMetadata().get("reservationId"))) {
            throw new RuntimeException("Ce paiement ne correspond pas à cette réservation");
        }

        boolean paiementReussi = STATUT_STRIPE_REUSSI.equals(paymentIntent.getStatus());
        boolean reservationExpiree = reservation.getStatut() == Reservation.StatutReservation.ANNULEE
                || !delaiEncoreValide(reservation);

        if (reservationExpiree && paiementReussi) {
            rembourser(paymentIntent, reservation);
            throw new RuntimeException("Le délai de paiement est dépassé : votre paiement a été remboursé");
        }

        verifierReservationPayable(reservation);

        if (STATUT_STRIPE_REFUSE.equals(paymentIntent.getStatus())) {
            prolongerDelai(reservation);
            throw new RuntimeException("Paiement refusé, veuillez réessayer");
        }

        if (!paiementReussi) {
            throw new RuntimeException("Le paiement n'a pas été confirmé par Stripe");
        }

        BigDecimal montantEnEuros = calculerMontantEnEuros(reservation);
        long montantEnCentimes = montantEnEuros.multiply(BigDecimal.valueOf(100)).longValueExact();

        if (!Long.valueOf(montantEnCentimes).equals(paymentIntent.getAmount())) {
            throw new RuntimeException("Le montant payé ne correspond pas au prix de la réservation");
        }

        marquerPlacesReservees(reservation);

        reservation.setStatut(Reservation.StatutReservation.PAYEE);
        reservationRepository.save(reservation);

        Billet billet = billetService.creerBillet(reservation);

        try {
            byte[] imageQrCode = qrCodeService.genererQrCode(billet.getQrCode());
            String sujet = "Confirmation de votre réservation - Le Grand Cinéma";
            String contenu = "Bonjour,\n\nVotre paiement a bien été reçu et votre billet est confirmé.\n\n"
                    + "Vous trouverez votre QR code en pièce jointe : présentez-le à l'entrée de la salle.\n\n"
                    + "Référence de votre billet : " + billet.getQrCode()
                    + "\n\nÀ bientôt au cinéma !";
            emailService.envoyerEmailAvecImage(reservation.getUtilisateur().getEmail(), sujet, contenu, imageQrCode, "billet-qrcode.png");
        } catch (RuntimeException exception) {
            LOGGER.warn("Échec de l'envoi de l'email de confirmation pour la réservation {}", reservation.getId(), exception);
        }

        return new PaiementResponse(
                reservation.getId(),
                "PAYEE",
                montantEnEuros,
                billet.getQrCode()
        );
    }

    private Reservation trouverReservationDuClient(Long reservationId, String emailUtilisateur) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!reservation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new ResourceNotFoundException("Réservation introuvable");
        }

        return reservation;
    }

    private void verifierReservationPayable(Reservation reservation) {
        if (reservation.getStatut() == Reservation.StatutReservation.PAYEE) {
            throw new RuntimeException("Cette réservation est déjà payée");
        }

        if (reservation.getStatut() == Reservation.StatutReservation.ANNULEE) {
            throw new RuntimeException("Cette réservation est annulée");
        }

        if (!delaiEncoreValide(reservation)) {
            throw new RuntimeException("Le délai de paiement est dépassé, veuillez refaire votre réservation");
        }
    }

    private boolean delaiEncoreValide(Reservation reservation) {
        List<Place> places = reservation.getPlaces();
        if (places == null || places.isEmpty()) {
            return false;
        }

        LocalDateTime maintenant = LocalDateTime.now();
        Long idClient = reservation.getUtilisateur().getId();

        return places.stream().allMatch(place ->
                place.getStatut() == Place.StatutPlace.VERROUILLEE
                        && place.getUtilisateurVerrouillage() != null
                        && idClient.equals(place.getUtilisateurVerrouillage().getId())
                        && place.getFinVerrouillage() != null
                        && place.getFinVerrouillage().isAfter(maintenant));
    }

    private void prolongerDelai(Reservation reservation) {
        LocalDateTime nouvelleFin = LocalDateTime.now().plusMinutes(delaiVerrouillageMinutes);
        for (Place place : reservation.getPlaces()) {
            place.setFinVerrouillage(nouvelleFin);
            placeRepository.save(place);
        }
    }

    private void marquerPlacesReservees(Reservation reservation) {
        for (Place place : reservation.getPlaces()) {
            place.setStatut(Place.StatutPlace.RESERVEE);
            place.setFinVerrouillage(null);
            place.setUtilisateurVerrouillage(null);
            placeRepository.save(place);
        }
    }

    private void rembourser(PaymentIntent paymentIntent, Reservation reservation) {
        try {
            RefundCreateParams parametres = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntent.getId())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("remboursement-" + paymentIntent.getId())
                    .build();

            Refund.create(parametres, options);
            LOGGER.info("Paiement {} remboursé : délai dépassé pour la réservation {}",
                    paymentIntent.getId(), reservation.getId());
        } catch (StripeException e) {
            LOGGER.error("Échec du remboursement du paiement {} pour la réservation {}",
                    paymentIntent.getId(), reservation.getId(), e);
            throw new RuntimeException(
                    "Le délai de paiement est dépassé et le remboursement automatique a échoué : contactez le cinéma");
        }
    }

    private BigDecimal calculerMontantEnEuros(Reservation reservation) {
        return reservation.getSeance().getPrix()
                .multiply(BigDecimal.valueOf(reservation.getPlaces().size()));
    }
}