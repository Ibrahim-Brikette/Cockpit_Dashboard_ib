package com.dynamicdashboard.cockpit.analytics.application.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsCountersDto {
    private Map<String, Long> dashboardViews;
    private Map<String, Long> widgetRawDataViews;
    private Map<String, Long> widgetInteractions;
    private Map<String, Long> queryExecutions;
    private long impressions;
    private long clicks;
}