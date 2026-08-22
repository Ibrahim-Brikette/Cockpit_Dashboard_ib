package com.dynamicdashboard.cockpit.sharing.application.dto;

import java.util.UUID;

public record ShareGrantDto(
    UUID   id,
    String shareLevel,
    String accessLevel,
    UUID   granteeUserId,
    UUID   granteeGroupId,
    String granteeName
) {}
