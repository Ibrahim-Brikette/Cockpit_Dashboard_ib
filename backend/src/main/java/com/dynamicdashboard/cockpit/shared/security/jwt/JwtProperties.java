package com.dynamicdashboard.cockpit.shared.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /** iss claim - Spec 5.2: "Exact domain... Validated on every request". */
    private String issuer;

    /** aud claim - Spec 5.2: "dashboard-cockpit-api". */
    private String audience;

    /** Spec 5.2: Access Token Expiry = 15 minutes. */
    private long accessTokenTtlMinutes;

    /** Spec 5.2: Refresh Token Expiry = 8 hours sliding, max 7 days. */
    private long refreshTokenTtlHours;
}
