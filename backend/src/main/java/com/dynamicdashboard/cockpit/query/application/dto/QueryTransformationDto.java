package com.dynamicdashboard.cockpit.query.application.dto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTransformationDto {
    private String id;
    private String type;
    private String fieldId;
    private String outputLabel;
    private String formula;
    private String format;
    private String replacementValue;
    private String operator;
    private String value;
}
