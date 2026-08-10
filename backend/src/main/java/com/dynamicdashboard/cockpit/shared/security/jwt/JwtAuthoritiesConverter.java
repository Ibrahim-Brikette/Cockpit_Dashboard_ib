package com.dynamicdashboard.cockpit.shared.security.jwt;

import com.dynamicdashboard.cockpit.identity.repository.RolePermissionRepository;
import com.dynamicdashboard.cockpit.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This is the "does the user have permission X" check made real - and it now
 * runs a live DB query to get there, on every single authenticated request.
 *
 * Called exactly once per request by Spring's JwtAuthenticationProvider, right
 * after signature/exp/iss/aud/jti-revocation validation and before your
 * controller runs. The JWT only carries "roles" (Spec 5.2's required claim set
 * has no "permissions" claim) - so roles are pulled off the token, then
 * flattened into permission codes via RolePermissionRepository.
 *
 * Returns JwtAuthenticationToken (Spring's purpose-built class for this),
 * NOT UsernamePasswordAuthenticationToken - JwtAuthenticationToken.getName()
 * correctly returns jwt.getSubject() (the user's UUID), which
 * DashboardPermissionEvaluator and any future code rely on.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = roleRepository.findPermissionsByUserId(UUID.fromString(jwt.getSubject()));

        // The live DB hit - one query, every request, resolving roles -> permissions.
        if (!roles.isEmpty()) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            roles.forEach(r -> System.out.println(r));
            List<String> permissions = rolePermissionRepository.findPermissionsByRolesNames(roles);
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
