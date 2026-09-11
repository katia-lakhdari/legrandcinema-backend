package com.legrandcinema.service;

import com.legrandcinema.dto.response.TauxRemplissageResponse;
import com.legrandcinema.entity.Place;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.repository.PlaceRepository;
import com.legrandcinema.repository.SeanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatistiqueService {

    private final SeanceRepository seanceRepository;
    private final PlaceRepository placeRepository;

    public List<TauxRemplissageResponse> calculerTauxRemplissage() {
        List<Seance> seances = seanceRepository.findAll();
        List<TauxRemplissageResponse> resultats = new ArrayList<>();

        for (Seance seance : seances) {
            List<Place> places = placeRepository.findBySeanceId(seance.getId());

            int placesReservees = 0;
            int placesBloquees = 0;

            for (Place place : places) {
                if (place.getStatut() == Place.StatutPlace.RESERVEE) {
                    placesReservees++;
                } else if (place.getStatut() == Place.StatutPlace.BLOQUEE) {
                    placesBloquees++;
                }
            }

            int placesDisponibles = places.size() - placesBloquees;
            double taux = placesDisponibles == 0
                    ? 0.0
                    : (placesReservees * 100.0) / placesDisponibles;

            resultats.add(new TauxRemplissageResponse(
                    seance.getId(),
                    seance.getFilm().getTitre(),
                    seance.getDateHeure(),
                    placesReservees,
                    placesDisponibles,
                    taux
            ));
        }

        return resultats;
    }
}