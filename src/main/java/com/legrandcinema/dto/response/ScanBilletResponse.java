package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScanBilletResponse {

    private String message;
    private String filmTitre;
    private String dateHeureSeance;
    private String clientNom;
}