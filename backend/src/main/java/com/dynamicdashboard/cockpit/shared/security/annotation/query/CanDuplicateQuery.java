package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Combines two rules:
 *   - must be able to read the source query (VIEW-level access on the resource)
 *   - must have the right to create new queries (authority check)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Query', 'query:view') and hasAuthority('query:create')")
public @interface CanDuplicateQuery {}
