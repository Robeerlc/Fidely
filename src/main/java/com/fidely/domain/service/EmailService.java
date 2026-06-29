package com.fidely.domain.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String name, String verificationUrl) {
        String html = "<h2>¡Hola, " + name + "!</h2>"
                + "<p>Gracias por registrar tu negocio en <strong>Fidely</strong>.</p>"
                + "<p>Para activar tu cuenta, confirma tu dirección de email:</p>"
                + "<br>"
                + "<a href='" + verificationUrl + "' style='background-color:#000;color:white;padding:12px 24px;"
                + "text-decoration:none;border-radius:8px;font-weight:bold;'>Verificar email</a>"
                + "<br><br>"
                + "<p style='color:#999;font-size:12px;'>Si no has creado esta cuenta, ignora este mensaje.</p>";
        send(toEmail, "Verifica tu email — Fidely", html);
    }

    @Async
    public void sendWelcomeAndCardEmail(String toEmail, String customerName, String brandName, String walletUrl) {
        String html = "<h1>¡Hola, " + customerName + "!</h1>"
                + "<p>Gracias por unirte a la familia de <strong>" + brandName + "</strong>.</p>"
                + "<p>Aquí tienes tu tarjeta de fidelización digital:</p>"
                + "<br>"
                + "<a href='" + walletUrl + "' style='background-color:#000;color:white;padding:12px 24px;"
                + "text-decoration:none;border-radius:8px;font-weight:bold;'>Añadir a Google/Apple Wallet</a>"
                + "<br><br><p>¡Nos vemos en tu próxima visita!</p>";
        send(toEmail, "¡Bienvenido a " + brandName + "! Aquí tienes tu tarjeta", html);
    }

    @Async
    public void sendMarketingEmail(String toEmail, String brandName, String subject, String body) {
        String html = "<div style='font-family:Arial,sans-serif;padding:20px;'>"
                + "<h2 style='color:#333;'>Novedades de " + brandName + "</h2>"
                + "<p style='font-size:16px;color:#555;'>" + body.replace("\n", "<br>") + "</p>"
                + "<hr style='border:1px solid #eee;margin-top:30px;'>"
                + "<p style='font-size:12px;color:#999;'>Has recibido este email porque eres cliente de " + brandName + ".</p>"
                + "</div>";
        send(toEmail, subject, html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
        }
    }
}
