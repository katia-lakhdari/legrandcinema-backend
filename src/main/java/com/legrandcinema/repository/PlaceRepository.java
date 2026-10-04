package com.legrandcinema.repository;

import com.legrandcinema.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findBySeanceId(Long seanceId);

    long countBySeanceIdAndStatut(Long seanceId, Place.StatutPlace statut);

    List<Place> findByStatutAndFinVerrouillageBefore(Place.StatutPlace statut, LocalDateTime date);
}