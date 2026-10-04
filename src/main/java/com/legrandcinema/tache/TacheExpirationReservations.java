package com.legrandcinema.tache;

import com.legrandcinema.service.ExpirationReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TacheExpirationReservations {

    private final ExpirationReservationService expirationReservationService;

    public TacheExpirationReservations(ExpirationReservationService expirationReservationService) {
        this.expirationReservationService = expirationReservationService;
    }

    @Scheduled(fixedDelay = 60000)
    public void libererVerrousExpires() {
        expirationReservationService.libererVerrousExpires();
    }
}