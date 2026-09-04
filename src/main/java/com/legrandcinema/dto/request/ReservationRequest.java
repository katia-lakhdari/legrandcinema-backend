package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReservationRequest {

    private Long seanceId;
    private List<Long> placeIds;
}