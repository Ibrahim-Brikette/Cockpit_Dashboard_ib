package com.dynamicdashboard.cockpit.audit.application.mapper;
import com.dynamicdashboard.cockpit.audit.application.dto.AuditEventDto;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import org.springframework.stereotype.Component;
@Component
public class AuditMapper {
    public AuditEventDto toDto(AuditEventEntity entity) {
        if (entity == null) return null;
        return AuditEventDto.builder()
                .id(entity.getId())
                // [IBR 2026-08-19] Fall back to "Système" for system-generated events (no human actor).
                // "Amine Haddad" was a development placeholder — removed.
                .actorName(entity.getActorUser() != null ? entity.getActorUser().getDisplayName() : "Système")
                .eventType(entity.getEventType())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .detailsJson(entity.getDetailsJson())
                .sourceIp(entity.getSourceIp())
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}
