package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ConnexionRequest;
import com.legrandcinema.dto.request.InscriptionRequest;
import com.legrandcinema.dto.request.MotDePasseOublieRequest;
import com.legrandcinema.dto.request.ReinitialisationMotDePasseRequest;
import com.legrandcinema.dto.response.AuthResponse;
import com.legrandcinema.dto.response.MessageResponse;
import com.legrandcinema.service.AuthService;
import com.legrandcinema.service.ReinitialisationMotDePasseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, mot de passe oublié")
public class AuthController {

    private final AuthService authService;
    private final ReinitialisationMotDePasseService reinitialisationMotDePasseService;

    public AuthController(AuthService authService,
                          ReinitialisationMotDePasseService reinitialisationMotDePasseService) {
        this.authService = authService;
        this.reinitialisationMotDePasseService = reinitialisationMotDePasseService;
    }

    @Operation(summary = "Créer un compte client")
    @PostMapping("/register")
    public AuthResponse inscrire(@Valid @RequestBody InscriptionRequest requete) {
        return authService.inscrire(requete);
    }

    @Operation(summary = "Se connecter et recevoir un token JWT")
    @PostMapping("/login")
    public AuthResponse connecter(@Valid @RequestBody ConnexionRequest requete) {
        return authService.connecter(requete);
    }

    @Operation(summary = "Demander un lien de réinitialisation du mot de passe")
    @PostMapping("/mot-de-passe-oublie")
    public MessageResponse demanderReinitialisation(@Valid @RequestBody MotDePasseOublieRequest requete) {
        return reinitialisationMotDePasseService.demanderReinitialisation(requete);
    }

    @Operation(summary = "Choisir un nouveau mot de passe")
    @PostMapping("/reinitialiser-mot-de-passe")
    public MessageResponse reinitialiserMotDePasse(@Valid @RequestBody ReinitialisationMotDePasseRequest requete) {
        return reinitialisationMotDePasseService.reinitialiserMotDePasse(requete);
    }
}