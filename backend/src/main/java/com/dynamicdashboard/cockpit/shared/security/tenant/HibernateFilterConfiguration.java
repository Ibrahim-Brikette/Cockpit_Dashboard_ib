package com.dynamicdashboard.cockpit.shared.security.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Sets the transaction interceptor order to LOWEST_PRECEDENCE - 100 so that it acts as the
 * outermost advice (opens the Hibernate Session first). TenantFilterAspect at
 * LOWEST_PRECEDENCE - 50 then runs *inside* the open transaction and can safely call
 * entityManager.unwrap(Session.class) to get the real, transaction-bound Session.
 */
@Configuration
@EnableTransactionManagement(order = Integer.MAX_VALUE - 100)
public class HibernateFilterConfiguration {
}
