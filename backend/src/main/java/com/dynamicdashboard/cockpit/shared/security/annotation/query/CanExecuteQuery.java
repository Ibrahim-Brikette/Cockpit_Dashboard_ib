package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Resource-level check — requires EXECUTE access on a specific query.
 * Used for single query execution endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Query', 'query:execute')")
public @interface CanExecuteQuery {}
