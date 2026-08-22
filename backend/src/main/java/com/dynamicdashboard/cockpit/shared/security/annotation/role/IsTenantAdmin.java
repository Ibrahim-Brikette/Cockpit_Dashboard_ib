package com.dynamicdashboard.cockpit.shared.security.annotation.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to TENANT_ADMIN or SUPER_ADMIN.
 * SUPER_ADMIN is always included — a super admin can do anything a tenant admin can.
 * Role names come from AppRole — update both if a role is renamed.
 * This composition lives here, not scattered across every endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
public @interface IsTenantAdmin {}
