package com.dynamicdashboard.cockpit.dashboard.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetFilterDto {
    private String id;
    private String label;
    private String fieldId;
    private String operator;
    private String value;
}
