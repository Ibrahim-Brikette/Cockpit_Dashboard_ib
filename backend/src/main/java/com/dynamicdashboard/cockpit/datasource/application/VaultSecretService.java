package com.dynamicdashboard.cockpit.datasource.application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Service
@RequiredArgsConstructor
@Slf4j
public class VaultSecretService {
    private final VaultTemplate vaultTemplate;
    private final Map<String, String> inMemorySecrets = new ConcurrentHashMap<>();
    public String storePassword(String password) {
        String secretPath = "secret/data/db-credentials/" + UUID.randomUUID();
        if (password != null) {
            inMemorySecrets.put(secretPath, password);
        }
        try {
            vaultTemplate.write(secretPath, Map.of("data", Map.of("password", password)));
            return secretPath;
        } catch (Exception e) {
            log.warn("Vault write failed on path {}; using in-memory fallback store: {}", secretPath, e.getMessage());
            return secretPath;
        }
    }
    public String retrievePassword(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return System.getenv().getOrDefault("COCKPIT_DB_PASSWORD", "postgres");
        }
        if (!secretKey.startsWith("secret/")) {
            return secretKey;
        }
        java.util.List<String> keysToTry = new java.util.ArrayList<>();
        keysToTry.add(secretKey);
        if (secretKey.contains("/data/")) {
            keysToTry.add(secretKey.replace("/data/", "/"));
        } else if (secretKey.startsWith("secret/")) {
            keysToTry.add(secretKey.replace("secret/", "secret/data/"));
        }
        for (String key : keysToTry) {
            try {
                VaultResponse response = vaultTemplate.read(key);
                if (response != null && response.getData() != null) {
                    Object dataObj = response.getData().get("data");
                    if (dataObj instanceof Map<?, ?> dataMap && dataMap.containsKey("password")) {
                        String pwd = (String) dataMap.get("password");
                        if (pwd != null && !pwd.isBlank()) return pwd;
                    }
                    if (response.getData().containsKey("password")) {
                        String pwd = (String) response.getData().get("password");
                        if (pwd != null && !pwd.isBlank()) return pwd;
                    }
                }
            } catch (Exception e) {
                log.warn("Vault read attempt failed for key {}; error: {}", key, e.getMessage());
            }
        }
        String inMem = inMemorySecrets.get(secretKey);
        if (inMem != null && !inMem.isBlank()) {
            return inMem;
        }
        return System.getenv().getOrDefault("COCKPIT_DB_PASSWORD", "postgres");
    }
}
