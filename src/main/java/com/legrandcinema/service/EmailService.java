package com.legrandcinema.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
        Email from = new Email(fromEmail);
        Email to = new Email(destinataire);
        Content content = new Content("text/plain", contenu);
        Mail mail = new Mail(from, sujet, to, content);

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