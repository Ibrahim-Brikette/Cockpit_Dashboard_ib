package com.dynamicdashboard.cockpit.analytics.application.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {
    private AnalyticsCountersDto counters;
    private List<AnalyticsDailyPointDto> daily;
    private List<AnalyticsEventDto> events;
}