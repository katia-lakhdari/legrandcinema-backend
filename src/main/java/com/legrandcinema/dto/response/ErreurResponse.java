package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErreurResponse {
    private String message;
    private int statut;
}