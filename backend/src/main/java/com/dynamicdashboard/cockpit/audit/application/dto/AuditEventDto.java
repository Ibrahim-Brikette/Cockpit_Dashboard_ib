package com.dynamicdashboard.cockpit.audit.application.dto;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto {
    private UUID id;
    private String actorName;
    private String eventType;
    private String targetType;
    private UUID targetId;
    private String detailsJson;
    private String sourceIp;
    private Instant occurredAt;
}
