package com.dynamicdashboard.cockpit.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cockpit.auth")
//❯ @ConfigurationProperties(prefix = "cockpit.auth")
public class CockpitAuthProperties {
    /**
     * STANDALONE = cockpit owns login, issues its own JWT.
     * INTEGRATED = mother app owns login, cockpit only validates incoming tokens.
     * Defaults to STANDALONE so the jar works out of the box.
     */
    private AuthMode mode = AuthMode.STANDALONE;
    /**
     * INTEGRATED only.
     * JWK endpoint of the mother app.
     * Example: https://auth.example.com/.well-known/jwks.json
     */
    private String jwkUri;

    private ClaimNames claims = new ClaimNames();

    public enum AuthMode {
        STANDALONE, INTEGRATED
    }

    @Getter
    @Setter
    public static class ClaimNames {
        private String userId = "sub";
        private String tenantId = "tenantId";
        private String roles = "roles";
        private String sessionId = "sessionId";

    }

}
