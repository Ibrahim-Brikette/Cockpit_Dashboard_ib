package com.dynamicdashboard.cockpit.shared.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externally-configured RSA key pair for STANDALONE mode.
 *
 * If private-key is set (via YAML or COCKPIT_RSA_PRIVATE_KEY env var), the key is
 * loaded once and reused across restarts — JWTs stay valid after a redeployment.
 *
 * If neither field is set, RsaKeyConfig falls back to in-memory generation with a
 * warning. Acceptable in local development; never acceptable in production.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cockpit.auth.rsa")
public class RsaKeyProperties {

    /**
     * PEM-encoded PKCS#8 RSA private key (BEGIN PRIVATE KEY).
     * Dev: paste content from the generated private.pem.
     * Prod: inject via COCKPIT_RSA_PRIVATE_KEY environment variable.
     */
    private String privateKey;

    /**
     * PEM-encoded X.509 RSA public key (BEGIN PUBLIC KEY).
     * Dev: paste content from the generated public.pem.
     * Prod: inject via COCKPIT_RSA_PUBLIC_KEY environment variable.
     */
    private String publicKey;
}
