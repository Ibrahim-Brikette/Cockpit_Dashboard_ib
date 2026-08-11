package com.dynamicdashboard.cockpit.alert.application;

import com.dynamicdashboard.cockpit.alert.application.dto.AlertChannelConfigDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertEventDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertRuleRequestDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertRuleResponseDto;
import com.dynamicdashboard.cockpit.alert.application.mapper.AlertMapper;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventChannelEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventChannelEntity.AlertEventChannelId;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertRuleChannelEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertRuleEntity;
import com.dynamicdashboard.cockpit.alert.repository.AlertEventChannelRepository;
import com.dynamicdashboard.cockpit.alert.repository.AlertEventRepository;
import com.dynamicdashboard.cockpit.alert.repository.AlertRuleChannelRepository;
import com.dynamicdashboard.cockpit.alert.repository.AlertRuleRepository;
import com.dynamicdashboard.cockpit.audit.application.AuditApplicationService;
import com.dynamicdashboard.cockpit.query.application.QueryApplicationService;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertComparisonOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertMetric;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertSeverity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertStatus;
import com.dynamicdashboard.cockpit.shared.utils.ParsingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertApplicationService {

    private final com.dynamicdashboard.cockpit.shared.cache.RedisCacheService redisCacheService;
    private final com.dynamicdashboard.cockpit.shared.sse.SseNotificationService sseNotificationService;


    private final AlertRuleRepository alertRuleRepository;
    private final AlertRuleChannelRepository alertRuleChannelRepository;
    private final AlertEventRepository alertEventRepository;
    private final AlertEventChannelRepository alertEventChannelRepository;
    private final DataQueryRepository dataQueryRepository;
    private final QueryApplicationService queryApplicationService;
    private final AlertMapper alertMapper;
    private final AuditApplicationService auditApplicationService;



    @Transactional(readOnly = true)
    public List<AlertRuleResponseDto> getAllRules() {
        List<AlertRuleResponseDto> cached = redisCacheService.get("alert:rules", new com.fasterxml.jackson.core.type.TypeReference<List<AlertRuleResponseDto>>() {});
        if (cached != null) return cached;
        List<AlertRuleResponseDto> result = alertRuleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(alertMapper::toRuleDto)
                .collect(Collectors.toList());
        redisCacheService.put("alert:rules", result);
        return result;
    }

    @Transactional
    public AlertRuleResponseDto createRule(AlertRuleRequestDto dto) {
        AlertRuleEntity rule = new AlertRuleEntity();
        applyDtoToRuleEntity(dto, rule);
        AlertRuleEntity saved = alertRuleRepository.save(rule);
        saveChannels(saved, dto.getChannels());

        auditApplicationService.logEvent("Création de règle d'alerte", "ALERT_RULE", saved.getId(), saved.getRuleName(), null);
        redisCacheService.evictByPrefix("alert");
        sseNotificationService.broadcast("alerts_changed");
        return alertMapper.toRuleDto(saved);
    }

    @Transactional
    public Optional<AlertRuleResponseDto> updateRule(UUID id, AlertRuleRequestDto dto) {
        return alertRuleRepository.findById(id).map(rule -> {
            applyDtoToRuleEntity(dto, rule);
            AlertRuleEntity saved = alertRuleRepository.save(rule);

            alertRuleChannelRepository.deleteAll(alertRuleChannelRepository.findByRuleId(saved.getId()));
            alertRuleChannelRepository.flush();
            saveChannels(saved, dto.getChannels());

            auditApplicationService.logEvent("Modification de règle d'alerte", "ALERT_RULE", saved.getId(), saved.getRuleName(), null);
            redisCacheService.evictByPrefix("alert");
            sseNotificationService.broadcast("alerts_changed");
            return alertMapper.toRuleDto(saved);
        });
    }

    @Transactional
    public boolean deleteRule(UUID id) {
        return alertRuleRepository.findById(id).map(rule -> {
            alertRuleChannelRepository.deleteAll(alertRuleChannelRepository.findByRuleId(id));
            
            List<AlertEventEntity> events = alertEventRepository.findByRuleId(id);
            for (AlertEventEntity event : events) {
                event.setRule(null);
            }
            alertEventRepository.saveAll(events);

            auditApplicationService.logEvent("Suppression de règle d'alerte", "ALERT_RULE", rule.getId(), rule.getRuleName(), null);
            alertRuleRepository.delete(rule);
            redisCacheService.evictByPrefix("alert");
            sseNotificationService.broadcast("alerts_changed");
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<AlertRuleResponseDto> toggleRule(UUID id) {
        return alertRuleRepository.findById(id).map(rule -> {
            rule.setEnabled(!rule.isEnabled());
            AlertRuleEntity saved = alertRuleRepository.save(rule);
            redisCacheService.evictByPrefix("alert");
            sseNotificationService.broadcast("alerts_changed");
            return alertMapper.toRuleDto(saved);
        });
    }

    private void applyDtoToRuleEntity(AlertRuleRequestDto dto, AlertRuleEntity rule) {
        rule.setRuleName(dto.getName() != null && !dto.getName().isBlank() ? dto.getName() : "Nouvelle règle");
        if (dto.getQueryId() != null && !dto.getQueryId().isBlank()) {
            UUID queryUuid = ParsingUtils.parseUuid(dto.getQueryId());
            DataQueryEntity query = queryUuid != null ? dataQueryRepository.findById(queryUuid).orElse(null) : null;
            rule.setQuery(query);
        } else {
            rule.setQuery(null);
        }
        rule.setMetric(ParsingUtils.parseEnum(AlertMetric.class, dto.getMetric(), AlertMetric.TOTAL));
        rule.setOperator(ParsingUtils.parseEnum(AlertComparisonOperator.class, dto.getOperator(), AlertComparisonOperator.GT));
        rule.setThreshold(dto.getThreshold() != null ? dto.getThreshold() : 0d);
        rule.setSeverity(ParsingUtils.parseEnum(AlertSeverity.class, dto.getSeverity(), AlertSeverity.WARNING));
        rule.setEnabled(dto.getEnabled() == null || dto.getEnabled());
    }

    private void saveChannels(AlertRuleEntity rule, List<AlertChannelConfigDto> channels) {
        if (channels == null) return;
        for (AlertChannelConfigDto channelDto : channels) {
            AlertRuleChannelEntity entity = new AlertRuleChannelEntity();
            entity.setRule(rule);
            entity.setChannel(AlertMapper.channelFromString(channelDto.getChannel()));
            entity.setEnabled(channelDto.isEnabled());
            entity.setRecipient(channelDto.getRecipient());
            alertRuleChannelRepository.save(entity);
        }
    }



    @Transactional(readOnly = true)
    public List<AlertEventDto> getAllEvents() {
        List<AlertEventDto> cached = redisCacheService.get("alert:events", new com.fasterxml.jackson.core.type.TypeReference<List<AlertEventDto>>() {});
        if (cached != null) return cached;
        List<AlertEventDto> result = alertEventRepository.findAllByOrderByTriggeredAtDesc().stream()
                .map(alertMapper::toEventDto)
                .collect(Collectors.toList());
        redisCacheService.put("alert:events", result);
        return result;
    }

    @Transactional
    public Optional<AlertEventDto> acknowledgeEvent(UUID id) {
        return alertEventRepository.findById(id).map(event -> {
            if (event.getStatus() != AlertStatus.RESOLVED) {
                event.setStatus(AlertStatus.ACKNOWLEDGED);
                event.setAcknowledgedAt(Instant.now());
                alertEventRepository.save(event);
                redisCacheService.evictByPrefix("alert");
                sseNotificationService.broadcast("alerts_changed");
            }
            return alertMapper.toEventDto(event);
        });
    }

    @Transactional
    public Optional<AlertEventDto> snoozeEvent(UUID id, int minutes) {
        return alertEventRepository.findById(id).map(event -> {
            if (event.getStatus() != AlertStatus.RESOLVED) {
                event.setStatus(AlertStatus.SNOOZED);
                event.setSnoozedUntil(Instant.now().plusSeconds((long) minutes * 60));
                alertEventRepository.save(event);
                redisCacheService.evictByPrefix("alert");
                sseNotificationService.broadcast("alerts_changed");
            }
            return alertMapper.toEventDto(event);
        });
    }



    @Transactional
    public List<AlertEventDto> evaluateAlerts() {
        List<AlertEventDto> triggered = new ArrayList<>();
        Instant now = Instant.now();

        List<AlertRuleEntity> enabledRules = alertRuleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(AlertRuleEntity::isEnabled)
                .collect(Collectors.toList());

        for (AlertRuleEntity rule : enabledRules) {
            DataQueryEntity query = rule.getQuery();
            if (query == null) continue;

            List<Map<String, Object>> rows = queryApplicationService.executeQueryData(query.getId(), null);
            Double observed = computeMetric(rows, rule.getMetric());
            if (observed == null) continue;

            boolean breached = switch (rule.getOperator()) {
                case GT -> observed > rule.getThreshold();
                case LT -> observed < rule.getThreshold();
                case EQ -> observed == rule.getThreshold();
            };

            Optional<AlertEventEntity> currentOpt =
                    alertEventRepository.findFirstByRuleIdAndStatusNotOrderByTriggeredAtDesc(rule.getId(), AlertStatus.RESOLVED);

            if (!breached) {
                currentOpt.ifPresent(current -> {
                    current.setStatus(AlertStatus.RESOLVED);
                    current.setResolvedAt(now);
                    current.setSnoozedUntil(null);
                    alertEventRepository.save(current);
                });
                continue;
            }

            if (currentOpt.isPresent()) {
                AlertEventEntity current = currentOpt.get();
                if (current.getStatus() == AlertStatus.SNOOZED
                        && current.getSnoozedUntil() != null
                        && !current.getSnoozedUntil().isAfter(now)) {
                    current.setStatus(AlertStatus.ACTIVE);
                    current.setTriggeredAt(now);
                    current.setSnoozedUntil(null);
                    current.setObservedValue(observed);
                    AlertEventEntity saved = alertEventRepository.save(current);
                    triggered.add(alertMapper.toEventDto(saved));
                }
                continue;
            }

            AlertEventEntity event = new AlertEventEntity();
            event.setRule(rule);
            event.setRuleName(rule.getRuleName());
            event.setQueryName(query.getQueryName());
            event.setSeverity(rule.getSeverity());
            event.setStatus(AlertStatus.ACTIVE);
            event.setMetric(rule.getMetric());
            event.setOperator(rule.getOperator());
            event.setThreshold(rule.getThreshold());
            event.setObservedValue(observed);
            event.setMessage(buildMessage(rule, query.getQueryName(), observed));
            event.setTriggeredAt(now);
            AlertEventEntity saved = alertEventRepository.save(event);

            List<AlertChannel> enabledChannels = alertRuleChannelRepository.findByRuleId(rule.getId()).stream()
                    .filter(AlertRuleChannelEntity::isEnabled)
                    .map(AlertRuleChannelEntity::getChannel)
                    .collect(Collectors.toList());
            for (AlertChannel channel : enabledChannels) {
                AlertEventChannelEntity link = new AlertEventChannelEntity();
                AlertEventChannelId linkId = new AlertEventChannelId();
                linkId.setEventId(saved.getId());
                linkId.setChannel(channel);
                link.setId(linkId);
                link.setEvent(saved);
                alertEventChannelRepository.save(link);
            }

            triggered.add(alertMapper.toEventDto(saved));
        }

        redisCacheService.evictByPrefix("alert");
        sseNotificationService.broadcast("alerts_changed");
        return triggered;
    }


    private Double computeMetric(List<Map<String, Object>> rows, AlertMetric metric) {
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            for (Object cell : row.values()) {
                if (cell instanceof Number number) {
                    values.add(number.doubleValue());
                }
            }
        }
        if (values.isEmpty()) return null;

        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        return switch (metric) {
            case AVERAGE -> total / values.size();
            case MAXIMUM -> values.stream().max(Double::compareTo).orElse(total);
            case MINIMUM -> values.stream().min(Double::compareTo).orElse(total);
            case TOTAL -> total;
        };
    }

    private String buildMessage(AlertRuleEntity rule, String queryName, double observed) {
        String metricLabel = switch (rule.getMetric()) {
            case TOTAL -> "Le total";
            case AVERAGE -> "La moyenne";
            case MAXIMUM -> "Le maximum";
            case MINIMUM -> "Le minimum";
        };
        String operatorLabel = switch (rule.getOperator()) {
            case GT -> "dépasse";
            case LT -> "est inférieur à";
            case EQ -> "atteint";
        };
        return String.format("%s de « %s » %s %.1f (%.1f observé)",
                metricLabel, queryName, operatorLabel, rule.getThreshold(), observed);
    }
}