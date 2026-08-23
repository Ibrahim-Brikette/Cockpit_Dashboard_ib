package com.dynamicdashboard.cockpit.catalog.application.dto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFieldDto {
    private UUID id;
    private String key;
    private String label;
    private String type;
    private String description;
    private boolean nullable;
}
