package com.dynamicdashboard.cockpit.query.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeQueryFilterDto {
    private String fieldId;
    private String operator;
    private String value;
}
