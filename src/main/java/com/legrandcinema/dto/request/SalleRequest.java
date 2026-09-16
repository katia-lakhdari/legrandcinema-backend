package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalleRequest {

    private String nom;
    private String type;
    private Integer capacite;
}