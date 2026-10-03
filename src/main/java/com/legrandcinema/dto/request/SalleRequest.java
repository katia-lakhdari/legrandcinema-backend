package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalleRequest {

    @NotBlank(message = "Le nom de la salle est obligatoire")
    private String nom;

    @NotBlank(message = "Le type de salle est obligatoire")
    private String type;

    @NotNull(message = "La capacité est obligatoire")
    @Positive(message = "La capacité doit être supérieure à 0")
    private Integer capacite;
}