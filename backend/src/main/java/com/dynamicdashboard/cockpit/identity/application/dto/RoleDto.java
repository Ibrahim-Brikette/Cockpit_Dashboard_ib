package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

public record RoleDto(UUID id, String roleName, String roleDescription) {}
