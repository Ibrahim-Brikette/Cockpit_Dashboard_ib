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
public class QuerySortDto {
    private String fieldId;
    private String direction;
}
