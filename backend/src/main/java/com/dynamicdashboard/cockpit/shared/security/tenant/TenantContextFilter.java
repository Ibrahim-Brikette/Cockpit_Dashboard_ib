package com.dynamicdashboard.cockpit.shared.security.tenant;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Extracts the tenantId from the JWT and stores it in TenantContext (ThreadLocal).
 * The Hibernate session filter is enabled separately by TenantFilterAspect, which runs
 * inside the @Transactional boundary where the real Hibernate Session is available.
 */
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final UserAccountRepository userAccountRepository;
   @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String tenantIdClaim = jwt.getClaimAsString("tenantId");

                if (tenantIdClaim == null || tenantIdClaim.isBlank()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT does not contain tenantId");
                    return;
                }
                UUID jwtTenantID = UUID.fromString(tenantIdClaim);
                ///                JwtAuthenticationToken.getName() returns jwt.getSubject() — the sub claim, nothing else
                ///                the client sends the JWT it got back, and JwtAuthoritiesConverter builds a fresh JwtAuthenticationToken
                ///                from scratch each time.
                ///               That's the only thing that ever gets written to SecurityContext,
                ///                for any Bearer-token request, ever.

                UserAccountEntity userAccountEntity = userAccountRepository.findById(UUID.fromString(authentication.getName())).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: "));

                if(!jwtTenantID.equals(userAccountEntity.getTenantId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant mismatch: JWT tenantId does not match user's assigned tenant");
                    return;

                }
                TenantContext.setCurrentTenant(jwtTenantID);
            }

            filterChain.doFilter(request, response);

        } finally {
            // VERY IMPORTANT: ThreadLocal must always be cleared to avoid leaking between requests
            TenantContext.clear();
        }
    }
}
