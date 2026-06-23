package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private static final String SENDER_EMAIL;
    private static final String APP_PASSWORD;

    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("credentials.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println(">>> EmailSender: credentials.properties not found — email will be disabled.");
        }
        SENDER_EMAIL = props.getProperty("gmail.sender_email", "");
        APP_PASSWORD = props.getProperty("gmail.app_password",  "");
    }

    public static void sendEmail(String recipientEmail, String subject, String body) {
        if (SENDER_EMAIL.isEmpty() || APP_PASSWORD.isEmpty()) {
            System.err.println(">>> Email skipped: Gmail credentials not configured.");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            System.out.println(">>> Transmitting email to " + recipientEmail + "...");
            Transport.send(message);
            System.out.println(">>> Email successfully sent!");

        } catch (MessagingException e) {
            System.err.println(">>> Email Failed to Send: " + e.getMessage());
        }
    }

    public static void sendHtmlEmail(String recipientEmail, String subject, String htmlBody) {
        if (SENDER_EMAIL.isEmpty() || APP_PASSWORD.isEmpty()) {
            System.err.println(">>> HTML Email skipped: Gmail credentials not configured.");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            System.out.println(">>> Transmitting HTML email to " + recipientEmail + "...");
            Transport.send(message);
            System.out.println(">>> HTML Email successfully sent!");

        } catch (MessagingException e) {
            System.err.println(">>> HTML Email Failed: " + e.getMessage());
        }
    }
}
