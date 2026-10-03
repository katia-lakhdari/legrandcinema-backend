package com.legrandcinema.dto.response;

import java.util.Map;

public record ErreurValidationResponse(
        String message,
        int statut,
        Map<String, String> erreurs
) {
}
