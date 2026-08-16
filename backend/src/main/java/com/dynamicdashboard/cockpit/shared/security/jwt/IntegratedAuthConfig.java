package com.dynamicdashboard.cockpit.shared.security.jwt;

import com.dynamicdashboard.cockpit.shared.security.CockpitAuthProperties;
import com.dynamicdashboard.cockpit.shared.security.auth.service.JtiRevocationService;
import com.dynamicdashboard.cockpit.shared.security.auth.service.JtiRevocationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Activated only when cockpit.auth.mode=INTEGRATED.
 * Provides a JwtDecoder that fetches the mother app's public key
 * from its JWK URI instead of using a locally generated RSA key.
 * No JwtEncoder — cockpit does not issue tokens in this mode.
 */

@Configuration
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "INTEGRATED")
@EnableConfigurationProperties({CockpitAuthProperties.class, JwtProperties.class})
@RequiredArgsConstructor

public class IntegratedAuthConfig {
    private final CockpitAuthProperties cockpitAuthProperties;
    private final JwtProperties jwtProperties;
    private final JtiRevocationService jtiRevocationService;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(cockpitAuthProperties.getJwkUri())
                .build();
        OAuth2TokenValidator<Jwt> defaults  = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        OAuth2TokenValidator<Jwt> audience  = new AudienceValidator(jwtProperties.getAudience());
        OAuth2TokenValidator<Jwt> jtiCheck  = new JtiRevocationValidator(jtiRevocationService);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience, jtiCheck));
        return decoder;
    }

}
