package com.legrandcinema.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalleRequest {

    @NotBlank(message = "Le nom de la salle est obligatoire")
    @Size(max = 255, message = "Le nom de la salle ne doit pas dépasser 255 caractères")
    private String nom;

    @NotBlank(message = "Le type de salle est obligatoire")
    private String type;

    @NotNull(message = "La capacité est obligatoire")
    @Positive(message = "La capacité doit être supérieure à 0")
    @Max(value = 520, message = "La capacité ne doit pas dépasser 520 places (26 rangées de 20)")
    private Integer capacite;
}