package com.legrandcinema.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;

@Service
public class EmailService {

    private final String sendgridApiKey;
    private final String fromEmail;

    public EmailService(@Value("${sendgrid.api.key}") String sendgridApiKey,
                        @Value("${sendgrid.from.email}") String fromEmail) {
        this.sendgridApiKey = sendgridApiKey;
        this.fromEmail = fromEmail;
    }

    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        Mail mail = construireMail(destinataire, sujet, contenu);
        envoyer(mail);
    }

    public void envoyerEmailAvecImage(String destinataire, String sujet, String contenu,
                                      byte[] image, String nomFichier) {
        Mail mail = construireMail(destinataire, sujet, contenu);

        Attachments pieceJointe = new Attachments();
        pieceJointe.setContent(Base64.getEncoder().encodeToString(image));
        pieceJointe.setType("image/png");
        pieceJointe.setFilename(nomFichier);
        pieceJointe.setDisposition("attachment");
        mail.addAttachments(pieceJointe);

        envoyer(mail);
    }

    private Mail construireMail(String destinataire, String sujet, String contenu) {
        Email from = new Email(fromEmail);
        Email to = new Email(destinataire);
        Content content = new Content("text/plain", contenu);
        return new Mail(from, sujet, to, content);
    }

    private void envoyer(Mail mail) {
        SendGrid sendGrid = new SendGrid(sendgridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sendGrid.api(request);
        } catch (IOException exception) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", exception);
        }
    }
}