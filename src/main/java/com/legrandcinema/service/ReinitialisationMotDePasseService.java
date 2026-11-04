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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ReinitialisationMotDePasseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReinitialisationMotDePasseService.class);

    private static final String MESSAGE_DEMANDE_ENVOYEE = "Si un compte existe pour cet email, un lien de réinitialisation vient d'être envoyé";
    private static final String MESSAGE_MOT_DE_PASSE_MODIFIE = "Votre mot de passe a bien été modifié";
    private static final String LIEN_INVALIDE_OU_EXPIRE = "Lien invalide ou expiré, veuillez refaire une demande";

    private final UtilisateurRepository utilisateurRepository;
    private final JetonReinitialisationRepository jetonReinitialisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final long dureeValiditeMinutes;
    private final String frontendUrl;

    public ReinitialisationMotDePasseService(UtilisateurRepository utilisateurRepository,
                                             JetonReinitialisationRepository jetonReinitialisationRepository,
                                             PasswordEncoder passwordEncoder,
                                             EmailService emailService,
                                             @Value("${reinitialisation.duree-validite-minutes}") long dureeValiditeMinutes,
                                             @Value("${frontend.url}") String frontendUrl) {
        this.utilisateurRepository = utilisateurRepository;
        this.jetonReinitialisationRepository = jetonReinitialisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.dureeValiditeMinutes = dureeValiditeMinutes;
        this.frontendUrl = frontendUrl;
    }

    public MessageResponse demanderReinitialisation(MotDePasseOublieRequest requete) {
        utilisateurRepository.findByEmail(requete.getEmail())
                .ifPresent(this::envoyerLienReinitialisation);

        return new MessageResponse(MESSAGE_DEMANDE_ENVOYEE);
    }

    @Transactional
    public MessageResponse reinitialiserMotDePasse(ReinitialisationMotDePasseRequest requete) {
        LocalDateTime maintenant = LocalDateTime.now();

        JetonReinitialisation jeton = jetonReinitialisationRepository
                .findByEmpreinteJeton(calculerEmpreinte(requete.getJeton()))
                .filter(jetonTrouve -> estValide(jetonTrouve, maintenant))
                .orElseThrow(() -> new RequeteInvalideException(LIEN_INVALIDE_OU_EXPIRE));

        Utilisateur utilisateur = jeton.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(requete.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);

        jeton.setUtilise(true);
        jetonReinitialisationRepository.save(jeton);

        return new MessageResponse(MESSAGE_MOT_DE_PASSE_MODIFIE);
    }

    private void envoyerLienReinitialisation(Utilisateur utilisateur) {
        LocalDateTime maintenant = LocalDateTime.now();

        invaliderJetonsEnCours(utilisateur, maintenant);

        String jetonEnClair = UUID.randomUUID().toString();

        JetonReinitialisation jeton = new JetonReinitialisation();
        jeton.setEmpreinteJeton(calculerEmpreinte(jetonEnClair));
        jeton.setUtilisateur(utilisateur);
        jeton.setDateExpiration(maintenant.plusMinutes(dureeValiditeMinutes));
        jetonReinitialisationRepository.save(jeton);

        String lien = frontendUrl + "/reinitialiser-mot-de-passe.html?jeton=" + jetonEnClair;

        try {
            String sujet = "Réinitialisation de votre mot de passe - Le Grand Cinéma";
            String contenu = "Bonjour,\n\nVous avez demandé à réinitialiser votre mot de passe.\n\n"
                    + "Cliquez sur ce lien pour choisir un nouveau mot de passe : " + lien
                    + "\n\nCe lien est valable " + dureeValiditeMinutes + " minutes et ne peut servir qu'une seule fois."
                    + "\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email."
                    + "\n\nÀ bientôt au cinéma !";
            emailService.envoyerEmail(utilisateur.getEmail(), sujet, contenu);
        } catch (RuntimeException exception) {
            LOGGER.warn("Échec de l'envoi de l'email de réinitialisation pour l'utilisateur {}", utilisateur.getId(), exception);
        }
    }

    private void invaliderJetonsEnCours(Utilisateur utilisateur, LocalDateTime maintenant) {
        List<JetonReinitialisation> jetonsEnCours = jetonReinitialisationRepository
                .findByUtilisateurIdAndUtiliseFalseAndDateExpirationAfter(utilisateur.getId(), maintenant);

        jetonsEnCours.forEach(jetonEnCours -> jetonEnCours.setUtilise(true));
        jetonReinitialisationRepository.saveAll(jetonsEnCours);
    }

    private boolean estValide(JetonReinitialisation jeton, LocalDateTime maintenant) {
        return !jeton.isUtilise() && jeton.getDateExpiration().isAfter(maintenant);
    }

    private String calculerEmpreinte(String jetonEnClair) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] empreinte = sha256.digest(jetonEnClair.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(empreinte);
        } catch (NoSuchAlgorithmException exception) {
            throw new ErreurTechniqueException("Erreur lors du calcul de l'empreinte du jeton", exception);
        }
    }
}