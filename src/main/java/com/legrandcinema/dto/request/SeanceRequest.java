package com.legrandcinema.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class SeanceRequest {

    @NotNull(message = "L'identifiant du film est obligatoire")
    private Long filmId;

    @NotNull(message = "L'identifiant de la salle est obligatoire")
    private Long salleId;

    @NotNull(message = "La date et l'heure sont obligatoires")
    @Future(message = "La date de la séance doit être dans le futur")
    private LocalDateTime dateHeure;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.50", message = "Le prix doit être d'au moins 0,50 €")
    @Digits(integer = 3, fraction = 2, message = "Le prix doit avoir au plus 3 chiffres avant la virgule et 2 après")
    private BigDecimal prix;

}