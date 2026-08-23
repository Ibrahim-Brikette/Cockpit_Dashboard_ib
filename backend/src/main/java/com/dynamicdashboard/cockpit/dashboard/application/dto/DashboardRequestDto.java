package com.dynamicdashboard.cockpit.dashboard.application.dto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRequestDto {
    private String id;
    private String name;
    private String description;
    private String color;
    private String status;
    private String owner;
    private String shareLevel;
    private Integer columns;
    private String density;
    private String refreshInterval;
    private List<WidgetDto> widgets;
    private List<GlobalFilterDto> globalFilters;
    private List<String> tags;
    private Boolean favorite;
    private Boolean archived;
}
