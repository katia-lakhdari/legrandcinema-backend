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
public class FilmRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
    private String titre;

    @NotBlank(message = "Le genre est obligatoire")
    @Size(max = 255, message = "Le genre ne doit pas dépasser 255 caractères")
    private String genre;

    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être un nombre positif")
    @Max(value = 600, message = "La durée ne doit pas dépasser 600 minutes")
    private Integer duree;

    @NotBlank(message = "L'affiche est obligatoire")
    @Size(max = 255, message = "Le lien de l'affiche ne doit pas dépasser 255 caractères")
    private String affiche;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
}