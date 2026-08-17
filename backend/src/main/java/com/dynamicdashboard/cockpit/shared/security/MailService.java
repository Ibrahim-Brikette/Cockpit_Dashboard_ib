package com.dynamicdashboard.cockpit.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin mail-sending service. Builds and dispatches plain-text emails.
 *
 * Three templates:
 *   sendLockoutEmail        — account locked by brute-force; always includes reset link
 *   sendForgotPasswordEmail — user-initiated reset request
 *   sendVerificationEmail   — new user account setup invitation (admin-created user)
 *
 * Failures are logged but never propagated — a mail outage must not roll back
 * the calling transaction or expose an error to the caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${cockpit.password-reset.base-url}")
    private String resetBaseUrl;

    public void sendLockoutEmail(String toEmail, String displayName,
                                  String rawToken, String lockDuration, boolean isPermanent) {
        String subject = isPermanent
                ? "Your account has been permanently locked"
                : "Your account has been locked for " + lockDuration;
        String resetLink = resetBaseUrl + "?token=" + rawToken;
        send(toEmail, subject, buildLockoutBody(displayName, resetLink, lockDuration, isPermanent));
    }

    public void sendForgotPasswordEmail(String toEmail, String displayName, String rawToken) {
        String resetLink = resetBaseUrl + "?token=" + rawToken;
        send(toEmail, "Reset your Cockpit password", buildForgotPasswordBody(displayName, resetLink));
    }

    /**
     * Sent when an admin creates a new user account (STANDALONE mode).
     *
     * The link points to the frontend's account-setup page.
     * The frontend extracts the token from the URL (?token=<value>) automatically
     * and includes it in the POST /api/auth/verify-email request body.
     * The user never types or even sees the token — they only fill in their password.
     *
     * TTL is 24h — the user may click the link the next morning.
     */
    public void sendVerificationEmail(String toEmail, String displayName, String rawToken) {
        // Uses a separate frontend route from password reset so the UI can show
        // the correct form ("Set up your account" instead of "Reset your password")
        String verifyLink = resetBaseUrl.replace("reset-password", "verify-email") + "?token=" + rawToken;
        send(toEmail, "Set up your Cockpit account", buildVerificationBody(displayName, verifyLink));
    }

    // ------------------------------------------------------------------
    // Private
    // ------------------------------------------------------------------

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        try {
            mailSender.send(msg);
            log.info("Email sent: to={} subject=\"{}\"", to, subject);
        } catch (Exception e) {
            log.error("Email send failed: to={} reason={}", to, e.getMessage());
        }
    }

    private String buildLockoutBody(String displayName, String resetLink,
                                     String lockDuration, boolean isPermanent) {
        StringBuilder b = new StringBuilder();
        b.append("Hello ").append(displayName).append(",\n\n");
        b.append("We detected multiple failed login attempts on your account.\n\n");
        if (isPermanent) {
            b.append("Your account has been PERMANENTLY LOCKED.\n");
            b.append("There is no automatic unlock — you must reset your password to regain access.\n\n");
        } else {
            b.append("Your account has been locked for ").append(lockDuration).append(".\n");
            b.append("It will unlock automatically after this period.\n\n");
        }
        b.append("Reset your password now to unlock immediately (link expires in 5 minutes):\n");
        b.append(resetLink).append("\n\n");
        b.append("If you did not attempt to log in, reset your password immediately.\n\n");
        b.append("— Cockpit Security Team");
        return b.toString();
    }

    private String buildForgotPasswordBody(String displayName, String resetLink) {
        StringBuilder b = new StringBuilder();
        b.append("Hello ").append(displayName).append(",\n\n");
        b.append("We received a request to reset your Cockpit password.\n\n");
        b.append("Click the link below to set a new password (link expires in 5 minutes):\n");
        b.append(resetLink).append("\n\n");
        b.append("If you did not request a password reset, you can safely ignore this email.\n");
        b.append("Your account has not been changed.\n\n");
        b.append("— Cockpit Security Team");
        return b.toString();
    }

    private String buildVerificationBody(String displayName, String verifyLink) {
        StringBuilder b = new StringBuilder();
        b.append("Hello ").append(displayName).append(",\n\n");
        b.append("An administrator has created a Cockpit account for you.\n\n");
        b.append("Click the link below to set your password and activate your account.\n");
        b.append("The link expires in 24 hours:\n");
        b.append(verifyLink).append("\n\n");
        b.append("If you were not expecting this invitation, you can safely ignore this email.\n");
        b.append("No account will be activated without your action.\n\n");
        b.append("— Cockpit Security Team");
        return b.toString();
    }
}
