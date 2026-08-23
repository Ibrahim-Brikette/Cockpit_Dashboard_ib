package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * Internal contract for domain-scoped permission evaluators.
 *
 * Each implementation handles exactly one targetType (e.g. "Dashboard", "Query").
 * Implementations must NOT implement Spring's PermissionEvaluator directly — that
 * interface is reserved for DelegatingPermissionEvaluator alone, which is the only
 * class registered with Spring Security's MethodSecurityExpressionHandler.
 *
 * To add a new domain: create a @Component that implements this interface and
 * returns the correct targetType from supportedTargetType(). The delegator
 * discovers it automatically via Spring's List injection — no other class changes.
 */
public interface DomainPermissionEvaluator {

    /** The targetType string this evaluator handles (e.g. "Dashboard", "Query"). */
    String supportedTargetType();

    boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission);
}
