package com.dynamicdashboard.cockpit.shared.security.auth.dto;

/**
 * Request body for POST /api/auth/verify-email.
 *
 * token       — the raw value from the email link (?token=<value>).
 *               The frontend extracts it from the URL query string automatically
 *               and puts it here. The user never types it manually.
 *
 * password    — the initial password the user chooses for their account.
 *               Must satisfy strength rules enforced by VerifyEmailService.
 */
public record VerifyEmailRequest(String token, String password) {}
