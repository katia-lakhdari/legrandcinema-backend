package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlocagePlaceRequest {

    @NotBlank(message = "La raison du blocage est obligatoire")
    private String raison;
}