package com.dynamicdashboard.cockpit.dashboard.application.mapper;
import com.dynamicdashboard.cockpit.dashboard.application.dto.DataGridConfigDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetFilterDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetLayoutDto;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridConfigEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetDatagridConfigRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetDatagridVisibleColumnRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class WidgetMapper {
    private final WidgetDatagridConfigRepository widgetDatagridConfigRepository;
    private final WidgetDatagridVisibleColumnRepository widgetDatagridVisibleColumnRepository;
    private final WidgetFilterRepository widgetFilterRepository;
    public WidgetDto toDto(WidgetEntity entity) {
        if (entity == null) return null;
        DataGridConfigDto dgDto = null;
        Optional<WidgetDatagridConfigEntity> dgOpt = widgetDatagridConfigRepository.findById(entity.getId());
        if (dgOpt.isPresent()) {
            WidgetDatagridConfigEntity dg = dgOpt.get();
            List<String> visCols = widgetDatagridVisibleColumnRepository.findByIdWidgetIdOrderByPositionIndexAsc(entity.getId())
                    .stream()
                    .map(vc -> vc.getId().getColumnName())
                    .collect(Collectors.toList());
            dgDto = DataGridConfigDto.builder()
                    .visibleColumns(visCols)
                    .rowsPerPage(dg.getRowsPerPage())
                    .density(dg.getDensity() != null ? dg.getDensity().name().toLowerCase() : "normal")
                    .showToolbar(dg.getShowToolbar())
                    .showSearch(dg.getShowSearch())
                    .showPagination(dg.getShowPagination())
                    .showTotals(dg.getShowTotals())
                    .sortable(dg.getSortable())
                    .filterable(dg.getFilterable())
                    .build();
        }
        List<WidgetFilterDto> filters = widgetFilterRepository.findByWidgetIdOrderByPositionIndexAsc(entity.getId())
                .stream()
                .map(f -> WidgetFilterDto.builder()
                        .id(f.getId() != null ? f.getId().toString() : null)
                        .label(f.getFilterLabel())
                        .fieldId(f.getTargetField() != null ? f.getTargetField().getFieldKey() : null)
                        .operator(f.getOperator() != null ? f.getOperator().name().toLowerCase() : "eq")
                        .value(f.getFilterValue())
                        .build())
                .collect(Collectors.toList());
        return WidgetDto.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .type(entity.getWidgetType() != null ? entity.getWidgetType().name().toLowerCase() : "kpi")
                .title(entity.getWidgetTitle())
                .showTitle(entity.isShowTitle())
                .description(entity.getWidgetDescription())
                .queryId(entity.getQuery() != null && entity.getQuery().getId() != null ? entity.getQuery().getId().toString() : null)
                .filters(filters.isEmpty() ? null : filters)
                .layout(new WidgetLayoutDto(entity.getGridX(), entity.getGridY(), entity.getGridW(), entity.getGridH()))
                .refreshInterval(entity.getRefreshInterval() != null ? entity.getRefreshInterval().name().toLowerCase() : "inherit")
                .navigateToDashboardId(entity.getNavigateToDashboard() != null && entity.getNavigateToDashboard().getId() != null ? entity.getNavigateToDashboard().getId().toString() : null)
                .kpiFormat(entity.getKpiFormat() != null ? entity.getKpiFormat().name().toLowerCase() : null)
                .datagrid(dgDto)
                .text(entity.getTextContent())
                .build();
    }
}
