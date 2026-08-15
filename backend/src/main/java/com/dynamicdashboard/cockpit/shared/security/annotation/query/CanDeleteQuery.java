package com.dynamicdashboard.cockpit.shared.security.annotation.query;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Resource-level check — requires DELETE access on the specific query.
 * Mapped to rank 3 (OWNER-tier) in DataQueryPermissionEvaluator.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Query', 'query:delete')")
public @interface CanDeleteQuery {}
