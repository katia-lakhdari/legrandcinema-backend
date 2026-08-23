package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class SeanceRequest {

    private Long filmId;
    private Long salleId;
    private LocalDateTime dateHeure;

}