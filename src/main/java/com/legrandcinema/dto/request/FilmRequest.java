package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilmRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "Le genre est obligatoire")
    private String genre;

    @Positive(message = "La durée doit être un nombre positif")
    private Integer duree;

    @NotBlank(message = "L'affiche est obligatoire")
    private String affiche;

    @NotBlank(message = "La description est obligatoire")
    private String description;
}