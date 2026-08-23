package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The single PermissionEvaluator registered with Spring Security.
 * Routes hasPermission(..., targetType, ...) calls to the matching DomainPermissionEvaluator.
 *
 * Domain evaluators are discovered automatically via the List<DomainPermissionEvaluator>
 * injected by Spring — adding a new domain evaluator requires no change here or in
 * SecurityConfiguration.
 */
public class DelegatingPermissionEvaluator implements PermissionEvaluator {

    // Old: map was built manually in SecurityConfiguration and passed in —
    // every new domain evaluator required a change in SecurityConfiguration.
    // private final Map<String, PermissionEvaluator> evaluators;
    // public DelegatingPermissionEvaluator(Map<String, PermissionEvaluator> evaluators) {
    //     this.evaluators = evaluators;
    // }

    private final Map<String, DomainPermissionEvaluator> evaluators;

    public DelegatingPermissionEvaluator(List<DomainPermissionEvaluator> domainEvaluators) {
        this.evaluators = domainEvaluators.stream()
                .collect(Collectors.toMap(DomainPermissionEvaluator::supportedTargetType, e -> e));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        DomainPermissionEvaluator evaluator = evaluators.get(targetType);
        if (evaluator == null) {
            return false;
        }
        return evaluator.hasPermission(authentication, targetId, targetType, permission);
    }
}
