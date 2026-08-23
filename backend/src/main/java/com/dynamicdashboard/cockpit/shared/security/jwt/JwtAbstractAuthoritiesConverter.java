package com.dynamicdashboard.cockpit.shared.security.jwt;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
public class JwtAbstractAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Object scopeClaim = jwt.getClaims().get("scope");
        if (scopeClaim instanceof String scope) {
            for (String value : scope.split(" ")) {
                if (!value.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + value));
                }
            }
        }
        Object scpClaim = jwt.getClaims().get("scp");
        if (scpClaim instanceof List<?> scopes) {
            for (Object scope : scopes) {
                if (scope != null) {
                    String normalized = scope.toString().trim();
                    if (!normalized.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + normalized));
                    }
                }
            }
        }
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object roles = realmAccessMap.get("roles");
            if (roles instanceof List<?> roleList) {
                for (Object role : roleList) {
                    if (role != null) {
                        String normalized = role.toString().trim();
                        if (!normalized.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized.toUpperCase()));
                        }
                    }
                }
            }
        }
        return authorities;
    }
}
