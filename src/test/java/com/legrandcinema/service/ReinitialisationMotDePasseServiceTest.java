package com.legrandcinema.service;

import com.legrandcinema.dto.request.MotDePasseOublieRequest;
import com.legrandcinema.dto.request.ReinitialisationMotDePasseRequest;
import com.legrandcinema.dto.response.MessageResponse;
import com.legrandcinema.entity.JetonReinitialisation;
import com.legrandcinema.entity.Utilisateur;
import com.legrandcinema.exception.ErreurTechniqueException;
import com.legrandcinema.exception.RequeteInvalideException;
import com.legrandcinema.repository.JetonReinitialisationRepository;
import com.legrandcinema.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReinitialisationMotDePasseServiceTest {

    private static final long DUREE_VALIDITE_MINUTES = 30L;
    private static final String FRONTEND_URL = "http://localhost:5500";
    private static final String DEBUT_LIEN = FRONTEND_URL + "/reinitialiser-mot-de-passe.html?jeton=";
    private static final String EMAIL = "client@test.com";
    private static final String JETON_EN_CLAIR = "3f2b8c1e-7d4a-4e9b-a1c6-5f0e2d9b8a71";
    private static final String NOUVEAU_MOT_DE_PASSE = "NouveauMotDePasse123";
    private static final String NOUVEAU_MOT_DE_PASSE_ENCODE = "nouveau-mot-de-passe-encode";
    private static final String MESSAGE_DEMANDE_ENVOYEE = "Si un compte existe pour cet email, un lien de réinitialisation vient d'être envoyé";
    private static final String MESSAGE_MOT_DE_PASSE_MODIFIE = "Votre mot de passe a bien été modifié";
    private static final String LIEN_INVALIDE_OU_EXPIRE = "Lien invalide ou expiré, veuillez refaire une demande";

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private JetonReinitialisationRepository jetonReinitialisationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private ReinitialisationMotDePasseService reinitialisationMotDePasseService;

    private Utilisateur client;
    private MotDePasseOublieRequest requeteDemande;
    private ReinitialisationMotDePasseRequest requeteReinitialisation;

    @BeforeEach
    void setUp() {
        reinitialisationMotDePasseService = new ReinitialisationMotDePasseService(
                utilisateurRepository,
                jetonReinitialisationRepository,
                passwordEncoder,
                emailService,
                DUREE_VALIDITE_MINUTES,
                FRONTEND_URL
        );

        client = new Utilisateur();
        client.setId(1L);
        client.setEmail(EMAIL);
        client.setMotDePasse("ancien-mot-de-passe-encode");
        client.setRole(Utilisateur.Role.CLIENT);

        requeteDemande = new MotDePasseOublieRequest();
        requeteDemande.setEmail(EMAIL);

        requeteReinitialisation = new ReinitialisationMotDePasseRequest();
        requeteReinitialisation.setJeton(JETON_EN_CLAIR);
        requeteReinitialisation.setNouveauMotDePasse(NOUVEAU_MOT_DE_PASSE);
    }

    private JetonReinitialisation creerJeton(LocalDateTime dateExpiration, boolean utilise) {
        JetonReinitialisation jeton = new JetonReinitialisation();
        jeton.setId(10L);
        jeton.setEmpreinteJeton(calculerEmpreinte(JETON_EN_CLAIR));
        jeton.setUtilisateur(client);
        jeton.setDateExpiration(dateExpiration);
        jeton.setUtilise(utilise);
        return jeton;
    }

    private String calculerEmpreinte(String jetonEnClair) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(jetonEnClair.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void demanderReinitialisation_emailConnu_enregistreLEmpreinteEtEnvoieLeLien() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(jetonReinitialisationRepository.findByUtilisateurIdAndUtiliseFalseAndDateExpirationAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of());

        LocalDateTime avant = LocalDateTime.now();
        MessageResponse reponse = reinitialisationMotDePasseService.demanderReinitialisation(requeteDemande);

        assertEquals(MESSAGE_DEMANDE_ENVOYEE, reponse.message());

        ArgumentCaptor<JetonReinitialisation> jetonCaptor = ArgumentCaptor.forClass(JetonReinitialisation.class);
        verify(jetonReinitialisationRepository).save(jetonCaptor.capture());
        JetonReinitialisation jetonEnregistre = jetonCaptor.getValue();

        ArgumentCaptor<String> contenuCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyerEmail(eq(EMAIL), anyString(), contenuCaptor.capture());
        String contenu = contenuCaptor.getValue();

        assertTrue(contenu.contains(DEBUT_LIEN));
        int debutJeton = contenu.indexOf(DEBUT_LIEN) + DEBUT_LIEN.length();
        String jetonDuLien = contenu.substring(debutJeton, debutJeton + 36);

        assertEquals(64, jetonEnregistre.getEmpreinteJeton().length());
        assertNotEquals(jetonDuLien, jetonEnregistre.getEmpreinteJeton());
        assertEquals(calculerEmpreinte(jetonDuLien), jetonEnregistre.getEmpreinteJeton());
        assertEquals(client, jetonEnregistre.getUtilisateur());
        assertFalse(jetonEnregistre.isUtilise());
        assertTrue(jetonEnregistre.getDateExpiration().isAfter(avant.plusMinutes(DUREE_VALIDITE_MINUTES - 1)));
        assertTrue(jetonEnregistre.getDateExpiration().isBefore(avant.plusMinutes(DUREE_VALIDITE_MINUTES + 1)));
    }

    @Test
    void demanderReinitialisation_emailInconnu_renvoieLeMemeMessageSansRienEnvoyer() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        MessageResponse reponse = reinitialisationMotDePasseService.demanderReinitialisation(requeteDemande);

        assertEquals(MESSAGE_DEMANDE_ENVOYEE, reponse.message());
        verify(jetonReinitialisationRepository, never()).save(any());
        verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    @Test
    void demanderReinitialisation_echecDeBrevo_renvoieQuandMemeLeMessage() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(jetonReinitialisationRepository.findByUtilisateurIdAndUtiliseFalseAndDateExpirationAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of());
        doThrow(new ErreurTechniqueException("Erreur lors de l'envoi de l'email", new RuntimeException("Brevo indisponible")))
                .when(emailService).envoyerEmail(eq(EMAIL), anyString(), anyString());

        MessageResponse reponse = reinitialisationMotDePasseService.demanderReinitialisation(requeteDemande);

        assertEquals(MESSAGE_DEMANDE_ENVOYEE, reponse.message());
        verify(jetonReinitialisationRepository).save(any(JetonReinitialisation.class));
    }

    @Test
    void demanderReinitialisation_anciensJetonsEncoreValides_sontInvalides() {
        JetonReinitialisation ancienJeton1 = creerJeton(LocalDateTime.now().plusMinutes(10), false);
        JetonReinitialisation ancienJeton2 = creerJeton(LocalDateTime.now().plusMinutes(20), false);
        List<JetonReinitialisation> anciensJetons = List.of(ancienJeton1, ancienJeton2);

        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(jetonReinitialisationRepository.findByUtilisateurIdAndUtiliseFalseAndDateExpirationAfter(eq(1L), any(LocalDateTime.class)))
                .thenReturn(anciensJetons);

        reinitialisationMotDePasseService.demanderReinitialisation(requeteDemande);

        assertTrue(ancienJeton1.isUtilise());
        assertTrue(ancienJeton2.isUtilise());
        verify(jetonReinitialisationRepository).saveAll(anciensJetons);
    }

    @Test
    void reinitialiserMotDePasse_jetonValide_changeLeMotDePasseEtBruleLeJeton() {
        JetonReinitialisation jeton = creerJeton(LocalDateTime.now().plusMinutes(10), false);
        when(jetonReinitialisationRepository.findByEmpreinteJeton(calculerEmpreinte(JETON_EN_CLAIR)))
                .thenReturn(Optional.of(jeton));
        when(passwordEncoder.encode(NOUVEAU_MOT_DE_PASSE)).thenReturn(NOUVEAU_MOT_DE_PASSE_ENCODE);

        MessageResponse reponse = reinitialisationMotDePasseService.reinitialiserMotDePasse(requeteReinitialisation);

        assertEquals(MESSAGE_MOT_DE_PASSE_MODIFIE, reponse.message());
        assertEquals(NOUVEAU_MOT_DE_PASSE_ENCODE, client.getMotDePasse());
        assertTrue(jeton.isUtilise());
        verify(utilisateurRepository).save(client);
        verify(jetonReinitialisationRepository).save(jeton);
    }

    @Test
    void reinitialiserMotDePasse_jetonExpire_leveRequeteInvalideException() {
        JetonReinitialisation jeton = creerJeton(LocalDateTime.now().minusMinutes(1), false);
        when(jetonReinitialisationRepository.findByEmpreinteJeton(calculerEmpreinte(JETON_EN_CLAIR)))
                .thenReturn(Optional.of(jeton));

        RequeteInvalideException exception = assertThrows(RequeteInvalideException.class,
                () -> reinitialisationMotDePasseService.reinitialiserMotDePasse(requeteReinitialisation));

        assertEquals(LIEN_INVALIDE_OU_EXPIRE, exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void reinitialiserMotDePasse_jetonDejaUtilise_leveRequeteInvalideException() {
        JetonReinitialisation jeton = creerJeton(LocalDateTime.now().plusMinutes(10), true);
        when(jetonReinitialisationRepository.findByEmpreinteJeton(calculerEmpreinte(JETON_EN_CLAIR)))
                .thenReturn(Optional.of(jeton));

        RequeteInvalideException exception = assertThrows(RequeteInvalideException.class,
                () -> reinitialisationMotDePasseService.reinitialiserMotDePasse(requeteReinitialisation));

        assertEquals(LIEN_INVALIDE_OU_EXPIRE, exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void reinitialiserMotDePasse_jetonInconnu_leveRequeteInvalideException() {
        when(jetonReinitialisationRepository.findByEmpreinteJeton(anyString())).thenReturn(Optional.empty());

        RequeteInvalideException exception = assertThrows(RequeteInvalideException.class,
                () -> reinitialisationMotDePasseService.reinitialiserMotDePasse(requeteReinitialisation));

        assertEquals(LIEN_INVALIDE_OU_EXPIRE, exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any());
    }
}