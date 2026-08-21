package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnexionRequest {

    private String email;
    private String motDePasse;
}