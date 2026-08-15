package com.dynamicdashboard.cockpit.shared.security.annotation.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 1 — Role annotation.
 * Restricts access to TENANT_ADMIN or SYSTEM_ADMIN.
 * SYSTEM_ADMIN is always included — a system admin can do anything a tenant admin can.
 * This composition lives here, not scattered across every endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
public @interface IsTenantAdmin {}
