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
    private String metric;     // total | average | maximum | minimum
    private String operator;   // gt | lt | eq
    private Double threshold;
    private String severity;   // critical | warning | info
    private Boolean enabled;
    private List<AlertChannelConfigDto> channels;
}