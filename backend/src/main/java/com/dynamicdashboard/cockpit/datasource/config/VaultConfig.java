package com.dynamicdashboard.cockpit.datasource.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import java.net.URI;
@Configuration
public class VaultConfig {
    @Value("${vault.uri:http://cockpit_vault:8200}")
    private String vaultUri;
    @Value("${vault.token:root}")
    private String vaultToken;
    @Bean
    public VaultEndpoint vaultEndpoint() {
        String uriToUse = (vaultUri != null && !vaultUri.isBlank() && vaultUri.contains("://"))
                ? vaultUri.trim()
                : "http://cockpit_vault:8200";
        try {
            String cleanUri = uriToUse.replace("http://", "").replace("https://", "");
            String[] parts = cleanUri.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1].split("/")[0]) : 8200;
            String scheme = uriToUse.startsWith("https") ? "https" : "http";
            VaultEndpoint endpoint = VaultEndpoint.create(host, port);
            endpoint.setScheme(scheme);
            return endpoint;
        } catch (Exception e) {
            VaultEndpoint endpoint = VaultEndpoint.create("cockpit_vault", 8200);
            endpoint.setScheme("http");
            return endpoint;
        }
    }
    @Bean
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultToken);
    }
    @Bean
    public VaultTemplate vaultTemplate(VaultEndpoint vaultEndpoint, ClientAuthentication clientAuthentication) {
        return new VaultTemplate(vaultEndpoint, clientAuthentication);
    }
}
