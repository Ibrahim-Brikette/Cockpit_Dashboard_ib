package com.dynamicdashboard.cockpit.alert.application.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleRequestDto {
    private String id;
    private String name;
    private String queryId;
    private String metric;
    private String operator;
    private Double threshold;
    private String severity;
    private Boolean enabled;
    private List<AlertChannelConfigDto> channels;
}