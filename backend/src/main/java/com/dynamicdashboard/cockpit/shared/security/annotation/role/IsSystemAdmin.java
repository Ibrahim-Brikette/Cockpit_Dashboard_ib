package com.dynamicdashboard.cockpit.shared.security.annotation.role;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to SUPER_ADMIN role only.
 * Role name comes from AppRole.SUPER_ADMIN — update both if the role is renamed.
 * If the rule ever changes, update it here — every endpoint using this annotation
 * is updated automatically.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SUPER_ADMIN')")
public @interface IsSystemAdmin {}
