package com.dynamicdashboard.cockpit.audit.application;
import com.dynamicdashboard.cockpit.audit.application.dto.AuditEventDto;
import com.dynamicdashboard.cockpit.audit.application.dto.CreateAuditEventRequestDto;
import com.dynamicdashboard.cockpit.audit.application.mapper.AuditMapper;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import com.dynamicdashboard.cockpit.audit.repository.AuditEventRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuditApplicationService {
    private final AuditEventRepository auditEventRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final AuditMapper auditMapper;
    @Transactional
    public AuditEventDto logAuditEvent(CreateAuditEventRequestDto dto) {
        UserAccountEntity user = currentUserService.getCurrentUser();
        String username = user != null ? user.getUsername() : "ahaddad";
        return logEvent(dto.getEventType(), dto.getTargetType(), dto.getTargetId(), dto.getDetailsJson(), username);
    }
    @Transactional
    public AuditEventDto logEvent(String eventType, String targetType, Object targetId, String detailsJson, String username) {
        UserAccountEntity actor = null;
        if (username != null && !username.isBlank()) {
            actor = userAccountRepository.findByUsername(username)
                    .or(() -> userAccountRepository.findByDisplayName(username))
                    .orElse(null);
        }
        if (actor == null) {
            actor = currentUserService.getCurrentUser();
        }
        AuditEventEntity event = new AuditEventEntity();
        event.setActorUser(actor);
        event.setEventType(eventType);
        event.setTargetType(targetType != null ? targetType : "DASHBOARD");
        if (targetId != null) {
            if (targetId instanceof java.util.UUID) {
                event.setTargetId((java.util.UUID) targetId);
            } else {
                try {
                    event.setTargetId(java.util.UUID.fromString(targetId.toString()));
                } catch (Exception ignored) {}
            }
        }
        event.setDetailsJson(detailsJson);
        event.setSourceIp("127.0.0.1");
        event.setUserAgent("Chrome");
        event.setOccurredAt(Instant.now());
        AuditEventEntity saved = auditEventRepository.save(event);
        return auditMapper.toDto(saved);
    }
    @Transactional(readOnly = true)
    public List<AuditEventDto> getRecentEvents() {
        return auditEventRepository.findAllByOrderByOccurredAtDesc()
                .stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }
}
