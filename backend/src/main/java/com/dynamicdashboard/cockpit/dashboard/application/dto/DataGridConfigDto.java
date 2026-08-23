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
public class DataGridConfigDto {
    private List<String> visibleColumns;
    private Integer rowsPerPage;
    private String density;
    private Boolean showToolbar;
    private Boolean showSearch;
    private Boolean showPagination;
    private Boolean showTotals;
    private Boolean sortable;
    private Boolean filterable;
}
