package com.legrandcinema.service;

import com.legrandcinema.dto.request.ConnexionRequest;
import com.legrandcinema.dto.request.InscriptionRequest;
import com.legrandcinema.dto.response.AuthResponse;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ConflitException;
import com.legrandcinema.repository.UtilisateurRepository;
import com.legrandcinema.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "client@test.com";
    private static final String MOT_DE_PASSE = "MotDePasse123";
    private static final String MOT_DE_PASSE_ENCODE = "mot-de-passe-encode";
    private static final String TOKEN = "token-test";

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Utilisateur client;

    @BeforeEach
    void setUp() {
        client = new Utilisateur();
        client.setPrenom("Lucas");
        client.setNom("Rinaldi");
        client.setEmail(EMAIL);
        client.setMotDePasse(MOT_DE_PASSE_ENCODE);
        client.setRole(Utilisateur.Role.CLIENT);
    }

    @Test
    void inscrire_emailLibre_creeUnClientEtRenvoieUnToken() {
        InscriptionRequest requete = new InscriptionRequest();
        requete.setPrenom("Lucas");
        requete.setNom("Rinaldi");
        requete.setEmail(EMAIL);
        requete.setMotDePasse(MOT_DE_PASSE);

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(MOT_DE_PASSE)).thenReturn(MOT_DE_PASSE_ENCODE);
        when(jwtUtil.genererToken(EMAIL, "CLIENT")).thenReturn(TOKEN);

        AuthResponse reponse = authService.inscrire(requete);

        assertEquals(TOKEN, reponse.getToken());
        assertEquals(EMAIL, reponse.getEmail());
        assertEquals("CLIENT", reponse.getRole());

        ArgumentCaptor<Utilisateur> captureur = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captureur.capture());
        Utilisateur enregistre = captureur.getValue();
        assertEquals(MOT_DE_PASSE_ENCODE, enregistre.getMotDePasse());
        assertEquals(Utilisateur.Role.CLIENT, enregistre.getRole());
    }

    @Test
    void inscrire_emailDejaUtilise_leveConflitException() {
        InscriptionRequest requete = new InscriptionRequest();
        requete.setEmail(EMAIL);

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));

        ConflitException exception = assertThrows(ConflitException.class,
                () -> authService.inscrire(requete));

        assertEquals("Un compte existe déjà avec cet email", exception.getMessage());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void connecter_identifiantsCorrects_renvoieUnToken() {
        ConnexionRequest requete = new ConnexionRequest();
        requete.setEmail(EMAIL);
        requete.setMotDePasse(MOT_DE_PASSE);

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(MOT_DE_PASSE, MOT_DE_PASSE_ENCODE)).thenReturn(true);
        when(jwtUtil.genererToken(EMAIL, "CLIENT")).thenReturn(TOKEN);

        AuthResponse reponse = authService.connecter(requete);

        assertEquals(TOKEN, reponse.getToken());
        assertEquals(EMAIL, reponse.getEmail());
        assertEquals("CLIENT", reponse.getRole());
    }

    @Test
    void connecter_emailInconnu_leveBadCredentialsException() {
        ConnexionRequest requete = new ConnexionRequest();
        requete.setEmail("inconnu@test.com");
        requete.setMotDePasse(MOT_DE_PASSE);

        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.connecter(requete));

        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
        verify(jwtUtil, never()).genererToken(anyString(), anyString());
    }

    @Test
    void connecter_motDePasseIncorrect_leveBadCredentialsException() {
        ConnexionRequest requete = new ConnexionRequest();
        requete.setEmail(EMAIL);
        requete.setMotDePasse("MauvaisMotDePasse");

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("MauvaisMotDePasse", MOT_DE_PASSE_ENCODE)).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.connecter(requete));

        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
        verify(jwtUtil, never()).genererToken(anyString(), anyString());
    }
}