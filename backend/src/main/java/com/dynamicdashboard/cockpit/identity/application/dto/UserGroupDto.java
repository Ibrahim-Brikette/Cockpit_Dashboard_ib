package com.dynamicdashboard.cockpit.identity.application.dto;

import java.util.UUID;

/**
 * User-centric view of a group membership.
 * Used when fetching "which groups does user X belong to?"
 * (The group-centric view is GroupMemberDto — "who is in group Y?")
 */
public record UserGroupDto(
    UUID   membershipId,
    UUID   groupId,
    String groupName,
    String groupCode,
    String membershipRole
) {}
