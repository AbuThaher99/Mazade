package com.Mazade.project.Core.Servecies;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;

    public void sendVerificationEmail(String to, String subject, String verificationUrl) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String content = "<p>Please click the button below to verify your email:</p>"
                + "<a href=\"" + verificationUrl + "\" style=\""
                + "display: inline-block; "
                + "font-size: 16px; "
                + "font-weight: bold; "
                + "color: #ffffff; "
                + "background-color: #007bff; "
                + "padding: 12px 24px; "
                + "text-align: center; "
                + "text-decoration: none; "
                + "border-radius: 4px; "
                + "border: 1px solid #007bff; "
                + "margin: 10px 0; "
                + "cursor: pointer; "
                + "box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); "
                + "\">Rest password</a>";

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        emailSender.send(message);
    }

    public void sentNotificationEmail(String to, String subject, String message) throws MessagingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(message, true);

        emailSender.send(mimeMessage);
    }
}