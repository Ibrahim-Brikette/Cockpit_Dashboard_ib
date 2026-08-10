package com.dynamicdashboard.cockpit.query.application.dto;
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
public class QueryResponseDto {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private String visibility;
    private List<String> sourceIds;
    private List<QueryJoinDto> joins;
    private List<String> selectedFieldIds;
    private List<QueryConditionDto> conditions;
    private List<QueryTransformationDto> transformations;
    private List<String> groupByFieldIds;
    private String aggregation;
    private String aggregationFieldId;
    private QuerySortDto sort;
    private int rowLimit;
    private int usedByWidgets;
    private Instant updatedAt;
}
