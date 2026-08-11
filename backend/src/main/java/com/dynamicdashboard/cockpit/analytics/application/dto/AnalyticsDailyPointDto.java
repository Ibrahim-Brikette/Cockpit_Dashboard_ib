package com.dynamicdashboard.cockpit.analytics.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDailyPointDto {
    private String date;
    private long activeUsers;
    private long dashboardViews;
    private long rawDataViews;
    private long clicks;
}