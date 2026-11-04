package com.legrandcinema.service;

import com.legrandcinema.dto.response.BilletResponse;
import com.legrandcinema.entity.Billet;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Reservation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ConflitException;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.BilletRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BilletService {

    private static final ZoneId FUSEAU_PARIS = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter FORMAT_SEANCE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    private final BilletRepository billetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final QrCodeService qrCodeService;

    public BilletService(BilletRepository billetRepository, UtilisateurRepository utilisateurRepository, QrCodeService qrCodeService) {
        this.billetRepository = billetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.qrCodeService = qrCodeService;
    }

    public Billet creerBillet(Reservation reservation) {
        if (billetRepository.existsByReservationId(reservation.getId())) {
            throw new ConflitException("Un billet existe déjà pour cette réservation");
        }

        Billet billet = new Billet();
        billet.setReservation(reservation);
        billet.setQrCode(UUID.randomUUID().toString());
        billet.setScanne(false);

        return billetRepository.save(billet);
    }

    public Billet scannerBillet(String qrCode) {
        Billet billet = billetRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Billet introuvable"));

        if (billet.isScanne()) {
            throw new ConflitException("Billet déjà utilisé");
        }

        LocalDateTime dateHeureSeance = billet.getReservation().getSeance().getDateHeure();
        LocalDate aujourdhui = LocalDate.now(FUSEAU_PARIS);

        if (!dateHeureSeance.toLocalDate().equals(aujourdhui)) {
            throw new ConflitException("Ce billet est pour la séance du "
                    + dateHeureSeance.format(FORMAT_SEANCE));
        }

        billet.setScanne(true);
        return billetRepository.save(billet);
    }

    public List<BilletResponse> mesBillets(String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return billetRepository.findByReservationUtilisateurId(utilisateur.getId()).stream()
                .map(this::construireReponse)
                .toList();
    }

    public byte[] obtenirImageQrCode(Long idBillet, String emailUtilisateur) {
        Billet billet = billetRepository.findById(idBillet)
                .orElseThrow(() -> new ResourceNotFoundException("Billet introuvable"));

        String emailProprietaire = billet.getReservation().getUtilisateur().getEmail();

        if (!emailProprietaire.equals(emailUtilisateur)) {
            throw new ResourceNotFoundException("Billet introuvable");
        }

        return qrCodeService.genererQrCode(billet.getQrCode());
    }

    private BilletResponse construireReponse(Billet billet) {
        Reservation reservation = billet.getReservation();
        List<String> numerosPlaces = reservation.getPlaces().stream()
                .map(Place::getNumero)
                .toList();

        return new BilletResponse(
                billet.getId(),
                reservation.getId(),
                billet.getQrCode(),
                billet.isScanne(),
                reservation.getUtilisateur().getPrenom() + " " + reservation.getUtilisateur().getNom(),
                reservation.getSeance().getFilm().getTitre(),
                reservation.getSeance().getDateHeure().toString(),
                reservation.getSeance().getSalle().getNom(),
                numerosPlaces
        );
    }
}