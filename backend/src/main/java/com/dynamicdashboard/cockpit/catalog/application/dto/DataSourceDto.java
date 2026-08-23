package com.dynamicdashboard.cockpit.catalog.application.dto;
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
public class DataSourceDto {
    private UUID id;
    private String key;
    private String label;
    private String description;
    private String app;
    private boolean active;
    private List<DataFieldDto> fields;
}
