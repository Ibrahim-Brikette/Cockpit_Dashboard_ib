package com.dynamicdashboard.cockpit.shared.security.auth.service;

public class IpThrottledException extends RuntimeException {
    public IpThrottledException() { super("Too many login attempts from this IP"); }
}
