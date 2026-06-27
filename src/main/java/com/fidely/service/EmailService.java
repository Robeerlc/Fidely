package com.fidely.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcomeAndCardEmail(String toEmail, String customerName, String brandName, String walletUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("¡Bienvenido a " + brandName + "! Aquí tienes tu tarjeta 🎁");

            String htmlBody = "<h1>¡Hola, " + customerName + "!</h1>"
                    + "<p>Gracias por unirte a la familia de <strong>" + brandName + "</strong>.</p>"
                    + "<p>Hemos generado tu tarjeta de fidelización digital. No necesitas instalar ninguna app nueva, "
                    + "simplemente guárdala en la cartera nativa de tu teléfono pulsando en el siguiente enlace:</p>"
                    + "<br>"
                    + "<a href='" + walletUrl + "' style='background-color:#000000; color:white; padding:12px 24px; text-decoration:none; border-radius:8px; font-weight:bold;'>Añadir a Google/Apple Wallet</a>"
                    + "<br><br>"
                    + "<p>¡Nos vemos en tu próxima visita!</p>";
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("Email de bienvenida enviado a: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error al enviar el email a " + toEmail + ": " + e.getMessage());
        }
    }
}