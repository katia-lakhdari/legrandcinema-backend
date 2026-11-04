package com.legrandcinema.repository;

import com.legrandcinema.entity.JetonReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JetonReinitialisationRepository extends JpaRepository<JetonReinitialisation, Long> {

    Optional<JetonReinitialisation> findByEmpreinteJeton(String empreinteJeton);

    List<JetonReinitialisation> findByUtilisateurIdAndUtiliseFalseAndDateExpirationAfter(Long utilisateurId, LocalDateTime maintenant);
}