package com.legrandcinema.repository;

import com.legrandcinema.entity.Film;
import com.legrandcinema.entity.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    @Query("SELECT s FROM Seance s WHERE s.film.id = :filmId AND s.dateHeure > :maintenant ORDER BY s.dateHeure ASC")
    List<Seance> trouverSeancesAVenirDuFilm(@Param("filmId") Long filmId, @Param("maintenant") LocalDateTime maintenant);

    @Query("SELECT DISTINCT s.film FROM Seance s WHERE s.dateHeure > :maintenant")
    List<Film> trouverFilmsAvecSeanceAVenir(@Param("maintenant") LocalDateTime maintenant);

    @Query("SELECT s FROM Seance s WHERE s.salle.id = :salleId")
    List<Seance> trouverSeancesDeLaSalle(@Param("salleId") Long salleId);
}