package com.legrandcinema.service;

import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlaceService {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

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
        place.setFinVerrouillage(LocalDateTime.now().plusMinutes(5));
        place.setUtilisateurVerrouillage(utilisateur);
        return placeRepository.save(place);
    }

    public Place libererPlace(Long id) {
        Place place = trouverParId(id);
        place.setStatut(Place.StatutPlace.LIBRE);
        place.setFinVerrouillage(null);
        place.setUtilisateurVerrouillage(null);
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

        if (estVerrouillee && estExpiree) {
            place.setStatut(Place.StatutPlace.LIBRE);
            place.setFinVerrouillage(null);
            place.setUtilisateurVerrouillage(null);
            placeRepository.save(place);
        }
    }
}