package com.dynamicdashboard.cockpit.sharing.application.dto;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccessLevel;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
import java.util.UUID;

public record CreateShareGrantRequest(
    ShareLevel  shareLevel,
    AccessLevel accessLevel,
    UUID        granteeUserId,
    UUID        granteeGroupId
) {}
