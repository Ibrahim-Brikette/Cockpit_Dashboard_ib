package com.dynamicdashboard.cockpit.alert.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertChannelConfigDto {
    private String channel;
    private boolean enabled;
    private String recipient;
}