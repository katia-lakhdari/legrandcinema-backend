package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ConnexionRequest;
import com.legrandcinema.dto.request.InscriptionRequest;
import com.legrandcinema.dto.response.AuthResponse;
import com.legrandcinema.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse inscrire(@Valid @RequestBody InscriptionRequest requete) {
        return authService.inscrire(requete);
    }

    @PostMapping("/login")
    public AuthResponse connecter(@Valid @RequestBody ConnexionRequest requete) {
        return authService.connecter(requete);
    }
}