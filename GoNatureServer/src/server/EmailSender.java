package server;

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

    // --- YOUR GMAIL CREDENTIALS HERE ---
    private static final String SENDER_EMAIL = "hamkh1221@gmail.com"; 
    private static final String APP_PASSWORD = "dbipyposejziyrxk"; // NO SPACES!

    public static void sendEmail(String recipientEmail, String subject, String body) {
        
        // 1. Setup the Google SMTP Server properties for a secure TLS connection
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // 2. Authenticate the GoNature Server with Google
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            // 3. Draft the Email
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            // 4. Send it over the internet!
            System.out.println(">>> Transmitting email to " + recipientEmail + "...");
            Transport.send(message);
            System.out.println(">>> Email successfully sent!");

        } catch (MessagingException e) {
            System.err.println(">>> Email Failed to Send: " + e.getMessage());
        }
    }
}