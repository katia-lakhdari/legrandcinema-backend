package com.legrandcinema.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final String emailExpediteur;

    public EmailService(JavaMailSender javaMailSender,
                        @Value("${email.expediteur}") String emailExpediteur) {
        this.javaMailSender = javaMailSender;
        this.emailExpediteur = emailExpediteur;
    }

    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        try {
            MimeMessage message = construireMessage(destinataire, sujet, contenu, null, null);
            envoyer(message);
        } catch (MessagingException exception) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", exception);
        }
    }

    public void envoyerEmailAvecImage(String destinataire, String sujet, String contenu,
                                      byte[] image, String nomFichier) {
        try {
            MimeMessage message = construireMessage(destinataire, sujet, contenu, image, nomFichier);
            envoyer(message);
        } catch (MessagingException exception) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", exception);
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

    private void envoyer(MimeMessage message) {
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", exception);
        }
    }
}