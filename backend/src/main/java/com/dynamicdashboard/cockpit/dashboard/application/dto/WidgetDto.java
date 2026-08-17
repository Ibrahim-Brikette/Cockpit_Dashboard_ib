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
public class WidgetDto {
    private String id;
    private String type;
    private String title;
    private boolean showTitle;
    private String description;
    private String queryId;
    private List<WidgetFilterDto> filters;
    private WidgetLayoutDto layout;
    private String refreshInterval;
    private String navigateToDashboardId;
    private String kpiFormat;
    private DataGridConfigDto datagrid;
    private String text;
}
