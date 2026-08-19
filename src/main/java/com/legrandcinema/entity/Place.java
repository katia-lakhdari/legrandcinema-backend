package com.legrandcinema.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @Column(nullable = false)
    private String numero; // ex: "A12"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPlace statut;

    private LocalDateTime verrouilleeJusqua;

    public enum StatutPlace {
        LIBRE,
        VERROUILLEE,
        RESERVEE,
        BLOQUEE
    }
}