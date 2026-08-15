package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * SYSTEM_ADMIN bypass OR standard query:create authority.
 * This is the exact composition that was previously inlined in QueryController.createQuery().
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('query:create')")
public @interface CanCreateQuery {}
