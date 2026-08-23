package com.legrandcinema.repository;

import com.legrandcinema.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findBySeanceId(Long seanceId);
}