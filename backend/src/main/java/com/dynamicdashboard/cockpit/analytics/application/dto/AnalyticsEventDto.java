package com.dynamicdashboard.cockpit.analytics.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventDto {
    private UUID id;
    private UUID userId;
    private String userName;
    private String action;
    private String target;
    private UUID targetId;
    private String targetName;
    private Instant timestamp;
    private UUID dashboardId;
    private String dashboardName;
}