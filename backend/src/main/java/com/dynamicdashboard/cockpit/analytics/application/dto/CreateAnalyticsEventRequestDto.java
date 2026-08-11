package com.dynamicdashboard.cockpit.analytics.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnalyticsEventRequestDto {
    private String action;
    private String target;
    private UUID targetId;
    private String targetName;
    private UUID dashboardId;
    private String dashboardName;
}