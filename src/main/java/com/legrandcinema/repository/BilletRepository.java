package com.legrandcinema.repository;

import com.legrandcinema.entity.Billet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BilletRepository extends JpaRepository<Billet, Long> {
}