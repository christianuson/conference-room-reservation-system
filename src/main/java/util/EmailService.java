package util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import model.Reservation;
import model.User;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Email service using Jakarta Mail API
 * Handles all email notifications for the conference room system
 */
public class EmailService {

    private static EmailService instance;
    private final Properties mailProperties;
    private final String fromEmail;
    private final String fromPassword;
    private final boolean enabled;

    private EmailService() {
        // Load email configuration from environment variables or config file
        this.fromEmail = System.getenv("MAIL_USERNAME");
        this.fromPassword = System.getenv("MAIL_PASSWORD");
        this.enabled = fromEmail != null && fromPassword != null;

        if (!enabled) {
            System.out.println("[EMAIL] Warning: Email credentials not configured. Email notifications disabled.");
            System.out.println("[EMAIL] Set MAIL_USERNAME and MAIL_PASSWORD environment variables to enable emails.");
        }

        // Configure mail properties for Gmail (modify for other providers)
        mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.host", System.getenv("MAIL_HOST") != null ?
                System.getenv("MAIL_HOST") : "smtp.gmail.com");
        mailProperties.put("mail.smtp.port", System.getenv("MAIL_PORT") != null ?
                System.getenv("MAIL_PORT") : "587");
        mailProperties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        mailProperties.put("mail.smtp.ssl.trust", "*");
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    /**
     * Send email asynchronously to avoid blocking UI thread
     */
    private CompletableFuture<Boolean> sendEmailAsync(String toEmail, String subject, String htmlBody) {
        return CompletableFuture.supplyAsync(() -> {
            if (!enabled) {
                System.out.println("[EMAIL] Email not sent (service disabled): " + subject);
                return false;
            }

            try {
                // Enable full Jakarta Mail debug logging
                mailProperties.put("mail.debug", "true");
                mailProperties.put("mail.smtp.starttls.required", "true");

                Session session = Session.getInstance(mailProperties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        System.out.println("[DEBUG] Authenticating as: " + fromEmail);
                        return new PasswordAuthentication(fromEmail, fromPassword);
                    }
                });

                System.out.println("[DEBUG] Preparing to send email to: " + toEmail);
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail, "Conference Room System"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=utf-8");

                // Send synchronously for now so we see exceptions clearly
                Transport.send(message);

                System.out.println("[EMAIL] ✅ Successfully sent email to: " + toEmail);
                System.out.println("[EMAIL] Subject: " + subject);
                return true;

            } catch (Exception e) {
                System.err.println("[EMAIL ERROR] ❌ Failed to send email to " + toEmail + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }).exceptionally(ex -> {
            System.err.println("[EMAIL ERROR] Async exception: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        });
    }


    /**
     * Send reservation confirmation email
     */
    public CompletableFuture<Boolean> sendReservationConfirmation(User user, Reservation reservation) {
        String subject = "Reservation Request Received - " + reservation.getRoomName();
        String htmlBody = EmailTemplate.reservationConfirmation(user, reservation);

        // Use user's actual email if available, otherwise construct from username
        String toEmail = user.getEmail() != null ? user.getEmail() : user.getUsername() + "@example.com";

        return sendEmailAsync(toEmail, subject, htmlBody);
    }

    /**
     * Send reservation approval email
     */
    public CompletableFuture<Boolean> sendReservationApproval(User user, Reservation reservation) {
        String subject = "Reservation Approved - " + reservation.getRoomName();
        String htmlBody = EmailTemplate.reservationApproved(user, reservation);

        String toEmail = user.getEmail() != null ? user.getEmail() : user.getUsername() + "@example.com";

        return sendEmailAsync(toEmail, subject, htmlBody);
    }

    /**
     * Send reservation rejection email
     */
    public CompletableFuture<Boolean> sendReservationRejection(User user, Reservation reservation) {
        String subject = "Reservation Rejected - " + reservation.getRoomName();
        String htmlBody = EmailTemplate.reservationRejected(user, reservation);

        String toEmail = user.getEmail() != null ? user.getEmail() : user.getUsername() + "@example.com";

        return sendEmailAsync(toEmail, subject, htmlBody);
    }

    /**
     * Send reservation cancellation email
     */
    public CompletableFuture<Boolean> sendReservationCancellation(User user, Reservation reservation) {
        String subject = "Reservation Cancelled - " + reservation.getRoomName();
        String htmlBody = EmailTemplate.reservationCancelled(user, reservation);

        String toEmail = user.getEmail() != null ? user.getEmail() : user.getUsername() + "@example.com";

        return sendEmailAsync(toEmail, subject, htmlBody);
    }

    /**
     * Send welcome email for new user registration
     */
    public CompletableFuture<Boolean> sendWelcomeEmail(User user) {
        String subject = "Welcome to Conference Room Reservation System";
        String htmlBody = EmailTemplate.welcomeEmail(user);

        return sendEmailAsync(user.getEmail(), subject, htmlBody);
    }

    /**
     * Check if email service is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}