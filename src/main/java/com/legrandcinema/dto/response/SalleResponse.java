package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalleResponse {

    private Long id;
    private String nom;
    private String type;
    private Integer capacite;
}