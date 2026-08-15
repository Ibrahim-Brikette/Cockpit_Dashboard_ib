package com.dynamicdashboard.cockpit.dashboard.application.dto;
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
public class DashboardResponseDto {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private String color;
    private String status;
    private String owner;
    private String shareLevel;
    private int columns;
    private String density;
    private String refreshInterval;
    private List<WidgetDto> widgets;
    private List<GlobalFilterDto> globalFilters;
    private List<String> tags;
    private boolean favorite;
    private boolean archived;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean sharedWithMe;
}
