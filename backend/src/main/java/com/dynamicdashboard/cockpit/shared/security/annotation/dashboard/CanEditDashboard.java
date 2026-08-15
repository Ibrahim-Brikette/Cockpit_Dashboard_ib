package com.dynamicdashboard.cockpit.shared.security.annotation.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Resource-level check — requires EDIT access on the specific dashboard.
 * Covers: update, archive, and any other mutation on an existing dashboard.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Dashboard', 'dashboard:edit')")
public @interface CanEditDashboard {}
