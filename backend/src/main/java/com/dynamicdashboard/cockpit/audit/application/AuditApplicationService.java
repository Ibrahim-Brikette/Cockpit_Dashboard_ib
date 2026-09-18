package com.dynamicdashboard.cockpit.audit.application;
import com.dynamicdashboard.cockpit.audit.application.dto.AuditEventDto;
import com.dynamicdashboard.cockpit.audit.application.dto.CreateAuditEventRequestDto;
import com.dynamicdashboard.cockpit.audit.application.mapper.AuditMapper;
import com.dynamicdashboard.cockpit.audit.domain.AuditEventEntity;
import com.dynamicdashboard.cockpit.audit.repository.AuditEventRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditApplicationService {

    /** Nom d'événement standard utilisé pour toutes les exécutions de requêtes (widgets et manuelles). */
    public static final String EVENT_QUERY_EXECUTION = "Exécution de requête";

    private final AuditEventRepository auditEventRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final AuditMapper auditMapper;

    @Transactional
    public AuditEventDto logAuditEvent(CreateAuditEventRequestDto dto) {
        UserAccountEntity user = currentUserService.getCurrentUser();
        String username = user != null ? user.getUsername() : null;
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
            try {
                actor = currentUserService.getCurrentUser();
            } catch (Exception e) {
                log.debug("Impossible de résoudre l'utilisateur courant pour l'audit: {}", e.getMessage());
            }
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
        event.setSourceIp(resolveClientIp());
        event.setUserAgent(resolveUserAgent());
        event.setOccurredAt(Instant.now());
        AuditEventEntity saved = auditEventRepository.save(event);
        return auditMapper.toDto(saved);
    }

    private String resolveClientIp() {
        HttpServletRequest request = currentHttpRequest();
        if (request == null) return "unknown";
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentHttpRequest();
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }

    private HttpServletRequest currentHttpRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getRecentEvents() {
        Instant threshold = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        return auditEventRepository.findTop200ByOccurredAtAfterOrderByOccurredAtDesc(threshold)
                .stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public long purgeOlderThan(Instant threshold) {
        long count = auditEventRepository.countByOccurredAtBefore(threshold);
        auditEventRepository.deleteByOccurredAtBefore(threshold);
        return count;
    }

    @Transactional(readOnly = true)
    public List<com.dynamicdashboard.cockpit.audit.application.dto.AuditTargetCountDto> getTopQueryExecutions(int limit) {
        return auditEventRepository
                .countGroupedByTarget(
                        EVENT_QUERY_EXECUTION,
                        org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(row -> com.dynamicdashboard.cockpit.audit.application.dto.AuditTargetCountDto.builder()
                        .targetId(row.getTargetId())
                        .label(row.getLabel())
                        .total(row.getTotal())
                        .build())
                .collect(Collectors.toList());
    }
}
