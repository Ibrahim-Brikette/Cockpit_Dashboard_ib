package com.dynamicdashboard.cockpit.shared.security.auth.dto;

public record PasswordResetRequest(String token, String newPassword) {}
