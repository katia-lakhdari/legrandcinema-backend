package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlocagePlaceRequest {

    @NotBlank(message = "La raison du blocage est obligatoire")
    @Size(max = 255, message = "La raison du blocage ne doit pas dépasser 255 caractères")
    private String raison;
}