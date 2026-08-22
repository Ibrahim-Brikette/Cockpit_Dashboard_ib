package com.dynamicdashboard.cockpit.identity.application.dto;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AssignmentScope;
import java.util.UUID;

public record AssignRoleRequest(UUID roleId, AssignmentScope assignmentScope) {}
