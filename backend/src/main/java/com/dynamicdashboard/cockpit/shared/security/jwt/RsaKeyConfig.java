package com.dynamicdashboard.cockpit.shared.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * DEV/TEST MODE: generates a fresh RSA key pair in memory on every startup -
 * used for signing real tokens if /login is ever called. Verification is
 * currently disabled entirely (see jwtDecoder below) for local Bearer-token
 * testing with jwt.io tokens.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class RsaKeyConfig {

    @Bean
    public RSAKey rsaJwk() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // ⚠ Signature, issuer, audience, expiry - NONE of it is checked here.
        // Any syntactically valid JWT is accepted, claims taken at face value.
        // Fine for local testing (paste a token from jwt.io as a Bearer token),
        // NOT safe anywhere else.
        return token -> {
            try {
                com.nimbusds.jwt.JWT parsed = com.nimbusds.jwt.JWTParser.parse(token);
                com.nimbusds.jwt.JWTClaimsSet claimsSet = parsed.getJWTClaimsSet();
                java.util.Map<String, Object> claims = claimsSet.getClaims();

                java.time.Instant issuedAt = claimsSet.getIssueTime() != null
                        ? claimsSet.getIssueTime().toInstant() : java.time.Instant.now();
                java.time.Instant expiresAt = claimsSet.getExpirationTime() != null
                        ? claimsSet.getExpirationTime().toInstant() : java.time.Instant.now().plusSeconds(3600);

                return new org.springframework.security.oauth2.jwt.Jwt(
                        token, issuedAt, expiresAt, java.util.Map.of("alg", "none"), claims);
            } catch (Exception e) {
                throw new org.springframework.security.oauth2.jwt.JwtException("Could not parse token", e);
            }
        };
    }
}