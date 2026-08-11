package com.dynamicdashboard.cockpit.analytics.application.mapper;

import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsEventDto;
import com.dynamicdashboard.cockpit.analytics.domain.AnalyticsEventEntity;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {

    public AnalyticsEventDto toDto(AnalyticsEventEntity entity) {
        if (entity == null) return null;

        return AnalyticsEventDto.builder()
                .id(entity.getId())
                .userId(entity.getActorUser() != null ? entity.getActorUser().getId() : null)
                .userName(entity.getActorUser() != null ? entity.getActorUser().getDisplayName() : "Utilisateur")
                .action(entity.getAction() != null ? entity.getAction().name().toLowerCase() : null)
                .target(entity.getTargetType() != null ? entity.getTargetType().name().toLowerCase() : null)
                .targetId(entity.getTargetId())
                .targetName(entity.getTargetName())
                .timestamp(entity.getOccurredAt())
                .dashboardId(entity.getDashboard() != null ? entity.getDashboard().getId() : null)
                .dashboardName(entity.getDashboardName())
                .build();
    }
}