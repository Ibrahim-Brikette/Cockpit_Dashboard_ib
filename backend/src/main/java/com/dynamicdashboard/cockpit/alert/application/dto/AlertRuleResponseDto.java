package com.dynamicdashboard.cockpit.alert.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponseDto {
    private UUID id;
    private String name;
    private String queryId;
    private String metric;
    private String operator;
    private double threshold;
    private String severity;
    private boolean enabled;
    private List<AlertChannelConfigDto> channels;
    private Instant createdAt;
    private Instant updatedAt;
}