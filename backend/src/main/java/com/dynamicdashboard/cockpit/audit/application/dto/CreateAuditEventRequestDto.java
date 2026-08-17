package com.dynamicdashboard.cockpit.audit.application.dto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditEventRequestDto {
    private String eventType;
    private String targetType;
    private UUID targetId;
    private String detailsJson;
    private String sourceIp;
}
