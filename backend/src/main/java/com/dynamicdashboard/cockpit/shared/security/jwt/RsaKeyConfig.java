package com.dynamicdashboard.cockpit.shared.security.jwt;

import com.dynamicdashboard.cockpit.shared.security.auth.service.JtiRevocationService;
import com.dynamicdashboard.cockpit.shared.security.auth.service.JtiRevocationValidator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * RSA-2048 key pair for RS256 token signing/verification.
 *
 * Key generation: in-memory on every startup (dev/staging acceptable — tokens
 * issued before a restart become invalid, which is safe).
 * Production note: load from a file or Vault secret so keys survive restarts.
 *
 * jwtDecoder change — was: no-op lambda that accepted ANY syntactically valid JWT
 * with zero validation (signature, issuer, audience, expiry all bypassed).
 * That was fine for jwt.io testing but contradicts spec 5.2 which mandates full
 * validation on every request. Replaced with NimbusJwtDecoder + four validators:
 *   1. Default Nimbus validators (signature + expiry)
 *   2. Issuer check  (spec 5.2: "exact domain, validated on every request")
 *   3. Audience check (spec 5.2: "dashboard-cockpit-api")
 *   4. JTI revocation check (spec 5.2: blacklist for logout / compromise)
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ JwtProperties.class, RsaKeyProperties.class })
@ConditionalOnProperty(name = "cockpit.auth.mode", havingValue = "STANDALONE" , matchIfMissing = true)
@RequiredArgsConstructor
public class  RsaKeyConfig {

    private final JwtProperties        jwtProperties;
    private final JtiRevocationService jtiRevocationService;
    private final RsaKeyProperties     rsaKeyProperties;

    @Bean
    public RSAKey rsaJwk() throws Exception {
        if (rsaKeyProperties.getPrivateKey() != null && !rsaKeyProperties.getPrivateKey().isBlank()) {
            // External key configured — persistent across restarts and shared across instances.
            log.info("RSA key loaded from configuration — tokens will survive restarts.");
            RSAPrivateKey privateKey = parsePrivateKey(rsaKeyProperties.getPrivateKey());
            RSAPublicKey  publicKey  = parsePublicKey(rsaKeyProperties.getPublicKey());
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID("cockpit-standalone-key")   // fixed ID — stable across restarts
                    .build();
        }

        // No key configured — generate in memory.
        // Acceptable in local development. Never use in production or multi-instance deployments.
        log.warn("No RSA key configured (cockpit.auth.rsa.private-key is blank). " +
                 "Generating in-memory key — tokens will be invalid after every restart. " +
                 "Set cockpit.auth.rsa.private-key and cockpit.auth.rsa.public-key for stable sessions.");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    /** Parses a PEM-encoded PKCS#8 private key (-----BEGIN PRIVATE KEY-----). */
    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    /** Parses a PEM-encoded X.509 public key (-----BEGIN PUBLIC KEY-----). */
    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }

//    JwtEncoder (uses PRIVATE key to SIGN tokens)
//                    used in AuthService.buildAccessToken()
//                     every login and refresh → signs the JWT

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaJwk) throws Exception {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(rsaJwk.toRSAPublicKey())
                .build();

        // Validator chain — all four must pass or the request is rejected with 401
        OAuth2TokenValidator<Jwt> defaults    = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        OAuth2TokenValidator<Jwt> audience    = new AudienceValidator(jwtProperties.getAudience());
        OAuth2TokenValidator<Jwt> jtiRevoked  = new JtiRevocationValidator(jtiRevocationService);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience, jtiRevoked));
        return decoder;
    }
}