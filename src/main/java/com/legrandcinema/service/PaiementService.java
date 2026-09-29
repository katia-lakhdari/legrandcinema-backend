package com.legrandcinema.service;

import com.legrandcinema.dto.request.ConfirmationPaiementRequest;
import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.IntentionPaiementResponse;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaiementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaiementService.class);

    private final ReservationRepository reservationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BilletService billetService;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    public PaiementService(ReservationRepository reservationRepository,
                           UtilisateurRepository utilisateurRepository,
                           BilletService billetService,
                           EmailService emailService,
                           QrCodeService qrCodeService) {
        this.reservationRepository = reservationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.billetService = billetService;
        this.emailService = emailService;
        this.qrCodeService = qrCodeService;
    }

    public IntentionPaiementResponse creerIntentionPaiement(PaiementRequest requete, String emailUtilisateur) {
        Reservation reservation = verifierReservationAvantPaiement(requete.getReservationId(), emailUtilisateur);

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

        return new IntentionPaiementResponse(
                reservation.getId(),
                paymentIntent.getId(),
                paymentIntent.getClientSecret(),
                montantEnEuros
        );
    }

    public PaiementResponse confirmerPaiement(ConfirmationPaiementRequest requete, String emailUtilisateur) {
        Reservation reservation = verifierReservationAvantPaiement(requete.getReservationId(), emailUtilisateur);

        BigDecimal montantEnEuros = calculerMontantEnEuros(reservation);
        long montantEnCentimes = montantEnEuros.multiply(BigDecimal.valueOf(100)).longValueExact();

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(requete.getPaymentIntentId());
        } catch (StripeException e) {
            throw new RuntimeException("Impossible de vérifier le paiement auprès de Stripe : " + e.getMessage());
        }

        if (!"succeeded".equals(paymentIntent.getStatus())) {
            throw new RuntimeException("Le paiement n'a pas été confirmé par Stripe");
        }

        if (!Long.valueOf(montantEnCentimes).equals(paymentIntent.getAmount())) {
            throw new RuntimeException("Le montant payé ne correspond pas au prix de la réservation");
        }

        if (!reservation.getId().toString().equals(paymentIntent.getMetadata().get("reservationId"))) {
            throw new RuntimeException("Ce paiement ne correspond pas à cette réservation");
        }

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

    private Reservation verifierReservationAvantPaiement(Long reservationId, String emailUtilisateur) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!reservation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Cette réservation ne vous appartient pas");
        }

        if (reservation.getStatut() == Reservation.StatutReservation.PAYEE) {
            throw new RuntimeException("Cette réservation est déjà payée");
        }

        if (reservation.getStatut() == Reservation.StatutReservation.ANNULEE) {
            throw new RuntimeException("Cette réservation est annulée");
        }

        return reservation;
    }

    private BigDecimal calculerMontantEnEuros(Reservation reservation) {
        return reservation.getSeance().getPrix()
                .multiply(BigDecimal.valueOf(reservation.getPlaces().size()));
    }
}