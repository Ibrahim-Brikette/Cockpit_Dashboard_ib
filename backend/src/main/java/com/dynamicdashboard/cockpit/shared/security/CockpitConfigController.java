package com.dynamicdashboard.cockpit.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class CockpitConfigController {

    private final CockpitAuthProperties cockpitAuthProperties;

    /**
     * GET /api/config
     *
     * Public — no auth required, available in both STANDALONE and INTEGRATED modes.
     * The frontend calls this once on startup to know which auth flow to render.
     */
    @GetMapping
    public ResponseEntity<ConfigResponse> getConfig() {
        return ResponseEntity.ok(new ConfigResponse(
                cockpitAuthProperties.getMode().name()
        ));
    }

    public record ConfigResponse(String mode) {}
}
