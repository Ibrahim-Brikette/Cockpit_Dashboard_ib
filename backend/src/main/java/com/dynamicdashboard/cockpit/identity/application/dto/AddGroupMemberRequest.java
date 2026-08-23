package com.dynamicdashboard.cockpit.identity.application.dto;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.MembershipRole;
import java.util.UUID;

public record AddGroupMemberRequest(UUID userId, MembershipRole membershipRole) {}
