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
public class AlertEventDto {
    private UUID id;
    private String ruleId;
    private String ruleName;
    private String queryName;
    private String severity;
    private String status;
    private String metric;
    private String operator;
    private double threshold;
    private double observedValue;
    private String message;
    private Instant triggeredAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    private Instant snoozedUntil;
    private List<String> deliveredChannels;
}