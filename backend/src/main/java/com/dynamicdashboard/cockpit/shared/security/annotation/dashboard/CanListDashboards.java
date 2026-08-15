package com.dynamicdashboard.cockpit.shared.security.annotation.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Authority-only check (no resource ID) — used for listing all dashboards.
 * Any user with the dashboard:view permission can list.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('dashboard:view')")
public @interface CanListDashboards {}
