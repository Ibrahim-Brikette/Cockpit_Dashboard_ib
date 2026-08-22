package com.dynamicdashboard.cockpit.dashboard.application.mapper;
import com.dynamicdashboard.cockpit.dashboard.application.dto.DashboardResponseDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.GlobalFilterDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetDto;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardTagRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class DashboardMapper {
    private final DashboardTagRepository dashboardTagRepository;
    private final GlobalFilterRepository globalFilterRepository;
    private final WidgetRepository widgetRepository;
    private final GlobalFilterMapper globalFilterMapper;
    private final WidgetMapper widgetMapper;
    public DashboardResponseDto toDto(DashboardEntity entity) {
        if (entity == null) return null;
        List<String> tags = dashboardTagRepository.findByIdDashboardId(entity.getId())
                .stream()
                .map(t -> t.getId().getTagValue())
                .collect(Collectors.toList());
        List<GlobalFilterDto> globalFilters = globalFilterRepository.findByDashboardId(entity.getId())
                .stream()
                .map(globalFilterMapper::toDto)
                .collect(Collectors.toList());
        List<WidgetDto> widgets = widgetRepository.findByDashboardId(entity.getId())
                .stream()
                .map(widgetMapper::toDto)
                .collect(Collectors.toList());
        return DashboardResponseDto.builder()
                .id(entity.getId())
                .name(entity.getDashboardName())
                .description(entity.getDashboardDescription())
                .color(entity.getColorHex())
                .status(entity.getStatus() != null ? entity.getStatus().name().toLowerCase() : "draft")
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .owner(entity.getOwner() != null ? entity.getOwner().getDisplayName() : "Vous")
                .shareLevel(entity.getShareLevel() != null ? entity.getShareLevel().name().toLowerCase() : "private")
                .columns(entity.getColumnsCount())
                .density(entity.getDensity() != null ? entity.getDensity().name().toLowerCase() : "compact")
                .refreshInterval(entity.getRefreshInterval() != null ? formatRefreshInterval(entity.getRefreshInterval()) : "off")
                .widgets(widgets)
                .globalFilters(globalFilters)
                .tags(tags)
                .favorite(entity.isFavorite())
                .archived(entity.isArchived())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .sharedWithMe(false)
                .tenantId(entity.getTenantId())
                .build();
    }
    private String formatRefreshInterval(com.dynamicdashboard.cockpit.shared.domain.DomainEnums.RefreshInterval interval) {
        if (interval == null) return "off";
        switch (interval) {
            case S5: return "5s";
            case S10: return "10s";
            case S30: return "30s";
            case M1: return "1min";
            case M5: return "5min";
            case M15: return "15min";
            case M30: return "30min";
            case H1: return "1h";
            default: return "off";
        }
    }
}
