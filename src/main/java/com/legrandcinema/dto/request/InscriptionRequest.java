package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InscriptionRequest {

    private String prenom;
    private String nom;
    private String email;
    private String motDePasse;
}