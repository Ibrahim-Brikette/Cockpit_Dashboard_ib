package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

public record GroupDto(UUID id, String groupName, String groupCode, String groupDescription) {}
