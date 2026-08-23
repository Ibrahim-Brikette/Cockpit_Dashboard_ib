package com.dynamicdashboard.cockpit.shared.security.auth.service;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) { super(message); }
}
