package com.legrandcinema.repository;

import com.legrandcinema.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalleRepository extends JpaRepository<Salle, Long> {
}