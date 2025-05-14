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

        String content = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Reset Your Password</title>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f7fa; color: #333333;\">\n" +
                "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);\">\n" +
                "        <!-- Header -->\n" +
                "        <tr>\n" +
                "            <td style=\"background-color: #5469d4; padding: 30px 20px; text-align: center;\">\n" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">\n" +
                "                    <tr>\n" +
                "                        <td style=\"text-align: center;\">\n" +
                "                            <div style=\"width: 60px; height: 60px; background-color: #ffffff; border-radius: 12px; display: inline-block; margin: 0 auto; line-height: 60px;\">\n" +
                "                                <img src=\"https://i.imgur.com/rLDYWTW.png\" alt=\"Lock Icon\" width=\"30\" style=\"vertical-align: middle;\">\n" +
                "                            </div>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"text-align: center; padding-top: 15px;\">\n" +
                "                            <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;\">Password Reset Request</h1>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "        <!-- Body -->\n" +
                "        <tr>\n" +
                "            <td style=\"padding: 30px 25px;\">\n" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 20px;\">\n" +
                "                            <p style=\"margin: 0; font-size: 16px; line-height: 1.5; color: #333333;\">Hello,</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 20px;\">\n" +
                "                            <p style=\"margin: 0; font-size: 16px; line-height: 1.5; color: #333333;\">We received a request to reset your password. If you didn't make this request, you can safely ignore this email.</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 25px;\">\n" +
                "                            <p style=\"margin: 0; font-size: 16px; line-height: 1.5; color: #333333;\">To reset your password, click on the button below:</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 30px; text-align: center;\">\n" +
                "                            <a href=\"" + verificationUrl + "\" style=\"display: inline-block; font-size: 16px; font-weight: 600; color: #ffffff; background-color: #5469d4; padding: 14px 32px; text-align: center; text-decoration: none; border-radius: 6px; border: none; margin: 10px 0; cursor: pointer; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); transition: all 0.3s ease;\">Reset Password</a>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 25px;\">\n" +
                "                            <p style=\"margin: 0; font-size: 16px; line-height: 1.5; color: #333333;\">If the button doesn't work, copy and paste the following link into your browser:</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 30px;\">\n" +
                "                            <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #5469d4; word-break: break-all;\">" + verificationUrl + "</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"border-top: 1px solid #e5e7eb; padding-top: 20px;\">\n" +
                "                            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f8fafc; border-radius: 6px; border-left: 4px solid #5469d4;\">\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 15px;\">\n" +
                "                                        <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #475569;\"><strong>Security Tip:</strong> For your security, this link will expire in 24 hours. Never share your password or this reset link with anyone.</p>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "        <!-- Footer -->\n" +
                "        <tr>\n" +
                "            <td style=\"background-color: #f5f7fa; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;\">\n" +
                "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 15px; text-align: center;\">\n" +
                "                            <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #64748b;\">If you didn't request a password reset, please contact support immediately.</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding-bottom: 10px; text-align: center;\">\n" +
                "                            <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #64748b;\">© 2025 Mazade. All rights reserved.</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";

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