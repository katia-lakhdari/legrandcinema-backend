package com.legrandcinema.service;

import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.ReservationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaiementService {

    private final ReservationRepository reservationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BilletService billetService;

    public PaiementService(ReservationRepository reservationRepository,
                           UtilisateurRepository utilisateurRepository,
                           BilletService billetService) {
        this.reservationRepository = reservationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.billetService = billetService;
    }

    public PaiementResponse traiterPaiement(PaiementRequest requete, String emailUtilisateur) {
        Reservation reservation = reservationRepository.findById(requete.getReservationId())
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

        BigDecimal montantEnEuros = reservation.getSeance().getPrix()
                .multiply(BigDecimal.valueOf(reservation.getPlaces().size()));
        long montantEnCentimes = montantEnEuros.multiply(BigDecimal.valueOf(100)).longValueExact();

        PaymentIntent paymentIntent;
        try {
            PaymentIntentCreateParams parametres = PaymentIntentCreateParams.builder()
                    .setAmount(montantEnCentimes)
                    .setCurrency("eur")
                    .setPaymentMethod("pm_card_visa")
                    .addPaymentMethodType("card")
                    .setConfirm(true)
                    .build();

            paymentIntent = PaymentIntent.create(parametres);
        } catch (StripeException e) {
            throw new RuntimeException("Le paiement a échoué : " + e.getMessage());
        }

        if (!"succeeded".equals(paymentIntent.getStatus())) {
            throw new RuntimeException("Le paiement n'a pas été confirmé par Stripe");
        }

        reservation.setStatut(Reservation.StatutReservation.PAYEE);
        reservationRepository.save(reservation);

        Billet billet = billetService.creerBillet(reservation);

        return new PaiementResponse(
                reservation.getId(),
                "PAYEE",
                montantEnEuros,
                billet.getQrCode()
        );
    }
}