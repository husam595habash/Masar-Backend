package com.masar.backend.infra;

import com.masar.backend.exception.EmailSendException;
import com.masar.backend.service.OtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends emails to users.
 */
@Service
public class EmailService implements OtpSender {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

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
     * Mail errors are turned into EmailSendException so callers do not depend on Spring Mail.
     */
    @Override
    public void sendOtp(String to, String otp) {
        try {
            sendEmail(to,
                    "Masar verification code",
                    "Your Masar verification code is: " + otp + "\n\n"
                            + "It expires in 5 minutes. If you did not create an account, ignore this email.");
        } catch (MailException e) {
            log.error("Could not send OTP email to {}", to, e);   // shows the real cause in the console
            throw new EmailSendException();
        }
    }
}
