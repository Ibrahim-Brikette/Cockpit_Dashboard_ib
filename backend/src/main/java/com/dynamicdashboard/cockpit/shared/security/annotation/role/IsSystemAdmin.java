package com.dynamicdashboard.cockpit.shared.security.annotation.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 1 — Role annotation.
 * Restricts access to SYSTEM_ADMIN role only.
 * If the rule ever changes, update it here — every endpoint using this annotation
 * is updated automatically.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public @interface IsSystemAdmin {}
