package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

public record GroupMemberDto(UUID membershipId, UUID userId, String displayName, String username, String membershipRole) {}
