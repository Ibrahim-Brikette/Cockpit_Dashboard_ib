package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * SUPER_ADMIN bypass OR standard query:create authority.
 * Role name comes from AppRole.SUPER_ADMIN — update both if the role is renamed.
 * This is the exact composition that was previously inlined in QueryController.createQuery().
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('query:create')")
public @interface CanCreateQuery {}
