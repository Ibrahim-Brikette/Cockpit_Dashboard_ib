package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Authority-only check — used for listing queries scoped to the current tenant.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('query:view')")
public @interface CanListQueries {}
