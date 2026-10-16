package com.masar.backend.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends emails to users.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;


    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Sends a plain-text email.
     */
    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * Sends the 6-digit verification code to the user.
     */
    public void sendOtp(String to, String otp) {
        sendEmail(to,
                "Masar verification code",
                "Your Masar verification code is: " + otp + "\n\n"
                        + "It expires in 5 minutes. If you did not create an account, ignore this email.");
    }
}
