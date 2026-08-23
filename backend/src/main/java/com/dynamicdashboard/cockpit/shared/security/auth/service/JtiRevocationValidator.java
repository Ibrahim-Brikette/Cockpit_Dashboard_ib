package com.dynamicdashboard.cockpit.shared.security.auth.service;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Plugged into the JWT decoder chain (see RsaKeyConfig.jwtDecoder) so that
 * blacklisted tokens are rejected at the same validation layer as signature,
 * expiry, issuer, and audience — before any controller or filter runs.
 *
 * Not a Spring bean itself; instantiated by RsaKeyConfig and handed the
 * JtiRevocationService singleton.
 */
public class JtiRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR =
        new OAuth2Error("invalid_token", "Token has been revoked", null);

    private final JtiRevocationService revocationService;

    public JtiRevocationValidator(JtiRevocationService revocationService) {
        this.revocationService = revocationService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();
        if (jti == null || revocationService.isRevoked(jti)) {
            return OAuth2TokenValidatorResult.failure(ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
