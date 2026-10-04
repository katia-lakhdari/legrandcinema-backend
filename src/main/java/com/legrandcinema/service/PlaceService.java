package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ResourceNotFoundException;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${reservation.delai-verrouillage-minutes}")
    private long delaiVerrouillageMinutes;

    public PlaceService(PlaceRepository placeRepository, UtilisateurRepository utilisateurRepository) {
        this.placeRepository = placeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Place> listerPlacesParSeance(Long seanceId) {
        List<Place> places = placeRepository.findBySeanceId(seanceId);
        for (Place place : places) {
            libererSiExpiree(place);
        }
        return places;
    }

    public Place verrouillerPlace(Long id, String emailUtilisateur) {
        Place place = trouverParId(id);

        if (place.getStatut() == Place.StatutPlace.RESERVEE) {
            throw new RuntimeException("Cette place est déjà réservée");
        }

        libererSiExpiree(place);

        if (place.getStatut() == Place.StatutPlace.VERROUILLEE) {
            throw new RuntimeException("Cette place est déjà en cours de sélection par un autre client");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        place.setStatut(Place.StatutPlace.VERROUILLEE);
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(delaiVerrouillageMinutes));
        place.setUtilisateurVerrouillage(utilisateur);
        return placeRepository.save(place);
    }

    public Place libererPlace(Long id, String emailUtilisateur) {
        Place place = trouverParId(id);

        if (place.getStatut() == Place.StatutPlace.RESERVEE) {
            throw new RuntimeException("Cette place est déjà réservée et payée, impossible de la libérer");
        }

        if (place.getReservation() != null) {
            throw new RuntimeException("Cette place fait partie d'une réservation en attente de paiement, impossible de la libérer seule");
        }

        libererSiExpiree(place);

        if (place.getStatut() != Place.StatutPlace.VERROUILLEE) {
            throw new RuntimeException("Cette place n'est pas verrouillée");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (place.getUtilisateurVerrouillage() == null
                || !place.getUtilisateurVerrouillage().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à libérer cette place");
        }

        place.setStatut(Place.StatutPlace.LIBRE);
        place.setFinVerrouillage(null);
        place.setUtilisateurVerrouillage(null);
        return placeRepository.save(place);
    }

    public Place bloquerPlace(Long id, String raison) {
        Place place = trouverParId(id);

        if (place.getStatut() == Place.StatutPlace.RESERVEE) {
            throw new RuntimeException("Impossible de bloquer une place déjà réservée");
        }

        libererSiExpiree(place);

        if (place.getStatut() == Place.StatutPlace.VERROUILLEE) {
            throw new RuntimeException("Impossible de bloquer une place en cours de sélection par un client");
        }

        place.setStatut(Place.StatutPlace.BLOQUEE);
        place.setRaisonBlocage(raison);
        return placeRepository.save(place);
    }

    public Place debloquerPlace(Long id) {
        Place place = trouverParId(id);

        if (place.getStatut() != Place.StatutPlace.BLOQUEE) {
            throw new RuntimeException("Cette place n'est pas bloquée");
        }

        place.setStatut(Place.StatutPlace.LIBRE);
        place.setRaisonBlocage(null);
        return placeRepository.save(place);
    }

    private Place trouverParId(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place introuvable"));
    }

    private void libererSiExpiree(Place place) {
        boolean estVerrouillee = place.getStatut() == Place.StatutPlace.VERROUILLEE;
        boolean estExpiree = place.getFinVerrouillage() != null
                && place.getFinVerrouillage().isBefore(LocalDateTime.now());
        boolean sansReservation = place.getReservation() == null;

        if (estVerrouillee && estExpiree && sansReservation) {
            place.setStatut(Place.StatutPlace.LIBRE);
            place.setFinVerrouillage(null);
            place.setUtilisateurVerrouillage(null);
            placeRepository.save(place);
        }
    }
}