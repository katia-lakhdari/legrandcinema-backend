package com.legrandcinema.service;

import com.legrandcinema.dto.request.InscriptionRequest;
import com.legrandcinema.dto.response.AuthResponse;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.legrandcinema.dto.request.ConnexionRequest;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse inscrire(InscriptionRequest requete) {
        if (utilisateurRepository.findByEmail(requete.getEmail()).isPresent()) {
            throw new RuntimeException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPrenom(requete.getPrenom());
        utilisateur.setNom(requete.getNom());
        utilisateur.setEmail(requete.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(requete.getMotDePasse()));
        utilisateur.setRole(Utilisateur.Role.CLIENT);

        utilisateurRepository.save(utilisateur);

        String token = jwtUtil.genererToken(utilisateur.getEmail(), utilisateur.getRole().name());

        return new AuthResponse(token, utilisateur.getEmail(), utilisateur.getRole().name());
    }

    public AuthResponse connecter(ConnexionRequest requete) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(requete.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(requete.getMotDePasse(), utilisateur.getMotDePasse())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        String token = jwtUtil.genererToken(utilisateur.getEmail(), utilisateur.getRole().name());

        return new AuthResponse(token, utilisateur.getEmail(), utilisateur.getRole().name());
    }
}