package com.dynamicdashboard.cockpit.shared.security.annotation.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Option 2 — Composed annotation.
 * Resource-level check — delegates to DashboardQueryPermissionEvaluator via the delegator.
 * #id is resolved from the annotated method's parameter named "id" at call time.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#id, 'Dashboard', 'dashboard:view')")
public @interface CanViewDashboard {}
