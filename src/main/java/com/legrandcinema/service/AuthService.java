package com.legrandcinema.service;

import com.legrandcinema.dto.request.ConnexionRequest;
import com.legrandcinema.dto.request.InscriptionRequest;
import com.legrandcinema.dto.response.AuthResponse;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ConflitException;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String IDENTIFIANTS_INCORRECTS = "Email ou mot de passe incorrect";

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
            throw new ConflitException("Un compte existe déjà avec cet email");
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
                .orElseThrow(() -> new BadCredentialsException(IDENTIFIANTS_INCORRECTS));

        if (!passwordEncoder.matches(requete.getMotDePasse(), utilisateur.getMotDePasse())) {
            throw new BadCredentialsException(IDENTIFIANTS_INCORRECTS);
        }

        String token = jwtUtil.genererToken(utilisateur.getEmail(), utilisateur.getRole().name());

        return new AuthResponse(token, utilisateur.getEmail(), utilisateur.getRole().name());
    }
}