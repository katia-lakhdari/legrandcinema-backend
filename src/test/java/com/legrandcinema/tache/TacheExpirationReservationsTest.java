package com.legrandcinema.tache;

import com.legrandcinema.service.ExpirationReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TacheExpirationReservationsTest {

    @Mock
    private ExpirationReservationService expirationReservationService;

    @InjectMocks
    private TacheExpirationReservations tacheExpirationReservations;

    @Test
    void libererVerrousExpires_appelleLeService() {
        tacheExpirationReservations.libererVerrousExpires();

        verify(expirationReservationService, times(1)).libererVerrousExpires();
    }

    @Test
    void libererVerrousExpires_estPlanifieeToutesLesMinutes() throws NoSuchMethodException {
        Scheduled planification = TacheExpirationReservations.class
                .getMethod("libererVerrousExpires")
                .getAnnotation(Scheduled.class);

        assertNotNull(planification);
        assertEquals(60000, planification.fixedDelay());
    }
}