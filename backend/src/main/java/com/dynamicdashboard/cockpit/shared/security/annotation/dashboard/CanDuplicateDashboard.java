package com.dynamicdashboard.cockpit.shared.security.annotation.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Combines two rules in one annotation:
 *   - must be able to read the source dashboard (EDIT-level access)
 *   - must have the right to create new dashboards (authority check)
 * This is Option 2's main value: a multi-condition rule expressed as a single readable name.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Dashboard', 'dashboard:edit') and hasAuthority('dashboard:create')")
public @interface CanDuplicateDashboard {}
