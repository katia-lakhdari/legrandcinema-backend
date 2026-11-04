package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanBilletRequest {

    @NotEmpty(message = "Le QR code est obligatoire")
    private String qrCode;
}