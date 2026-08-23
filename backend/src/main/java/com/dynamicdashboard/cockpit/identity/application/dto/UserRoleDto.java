package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

public record UserRoleDto(UUID assignmentId, UUID roleId, String roleName, String assignmentScope) {}
