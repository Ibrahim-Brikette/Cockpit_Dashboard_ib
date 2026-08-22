package com.dynamicdashboard.cockpit.shared.security.tenant;

import com.dynamicdashboard.cockpit.shared.security.authorization.AppRole;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enables the Hibernate "tenantFilter" on the transaction-bound Session before any
 * @Transactional service method executes its queries.
 *
 * This must run INSIDE the transaction (after the Session is opened), which is achieved by:
 * - @EnableTransactionManagement(order = LOWEST_PRECEDENCE - 100) in HibernateFilterConfiguration
 *   → transaction interceptor is outermost (opens Session first)
 * - This aspect at @Order(LOWEST_PRECEDENCE - 50) is therefore inner → Session already exists here
 *
 * SUPER_ADMIN bypasses the tenant filter — system-wide access, no tenant scope.
 * Role string comes from AppRole.SUPER_ADMIN.springRole() — update AppRole if renamed.
 */
@Aspect
@Component
@Order(Integer.MAX_VALUE - 50)
@RequiredArgsConstructor
public class TenantFilterAspect {

    private static final String TENANT_FILTER_NAME = "tenantFilter";
    // Derived from AppRole enum — not a hardcoded string literal.
    private static final String SUPER_ADMIN_ROLE = AppRole.SUPER_ADMIN.springRole();

    private final EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) " +
            "&& within(com.dynamicdashboard.cockpit..*)")
    public void enableTenantFilter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> SUPER_ADMIN_ROLE.equals(a.getAuthority()))
        ){
            return;
        }
        UUID tenantId = TenantContext.CURRENT_TENANT.get();
        if (tenantId == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(TENANT_FILTER_NAME)
               .setParameter("tenantId", tenantId);
    }
}
