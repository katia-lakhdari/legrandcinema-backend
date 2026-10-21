package com.legrandcinema.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final String ERREUR_ENVOI_EMAIL = "Échec de l'envoi de l'email : {}";

    private final JavaMailSender javaMailSender;
    private final String emailExpediteur;

    public EmailService(JavaMailSender javaMailSender,
                        @Value("${email.expediteur}") String emailExpediteur) {
        this.javaMailSender = javaMailSender;
        this.emailExpediteur = emailExpediteur;
    }

    @Async
    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        envoyer(destinataire, sujet, contenu, null, null);
    }

    @Async
    public void envoyerEmailAvecImage(String destinataire, String sujet, String contenu,
                                      byte[] image, String nomFichier) {
        envoyer(destinataire, sujet, contenu, image, nomFichier);
    }

    private void envoyer(String destinataire, String sujet, String contenu,
                         byte[] image, String nomFichier) {
        try {
            MimeMessage message = construireMessage(destinataire, sujet, contenu, image, nomFichier);
            javaMailSender.send(message);
        } catch (MessagingException | MailException exception) {
            LOGGER.warn(ERREUR_ENVOI_EMAIL, sujet, exception);
        }
    }

    private MimeMessage construireMessage(String destinataire, String sujet, String contenu,
                                          byte[] image, String nomFichier) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(emailExpediteur);
        helper.setTo(destinataire);
        helper.setSubject(sujet);
        helper.setText(contenu, false);

        if (image != null) {
            helper.addAttachment(nomFichier, new ByteArrayResource(image), "image/png");
        }

        return message;
    }
}