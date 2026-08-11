package com.dynamicdashboard.cockpit.alert.application.mapper;

import com.dynamicdashboard.cockpit.alert.application.dto.AlertChannelConfigDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertEventDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertRuleResponseDto;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventChannelEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertEventEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertRuleChannelEntity;
import com.dynamicdashboard.cockpit.alert.domain.AlertRuleEntity;
import com.dynamicdashboard.cockpit.alert.repository.AlertEventChannelRepository;
import com.dynamicdashboard.cockpit.alert.repository.AlertRuleChannelRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertMapper {

    private final AlertRuleChannelRepository alertRuleChannelRepository;
    private final AlertEventChannelRepository alertEventChannelRepository;

    public AlertRuleResponseDto toRuleDto(AlertRuleEntity entity) {
        if (entity == null) return null;

        List<AlertChannelConfigDto> channels = alertRuleChannelRepository.findByRuleId(entity.getId())
                .stream()
                .map(this::toChannelDto)
                .collect(Collectors.toList());

        return AlertRuleResponseDto.builder()
                .id(entity.getId())
                .name(entity.getRuleName())
                .queryId(entity.getQuery() != null ? entity.getQuery().getId().toString() : null)
                .metric(entity.getMetric().name().toLowerCase())
                .operator(entity.getOperator().name().toLowerCase())
                .threshold(entity.getThreshold())
                .severity(entity.getSeverity().name().toLowerCase())
                .enabled(entity.isEnabled())
                .channels(channels)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AlertEventDto toEventDto(AlertEventEntity entity) {
        if (entity == null) return null;

        List<String> deliveredChannels = alertEventChannelRepository.findByIdEventId(entity.getId())
                .stream()
                .map(c -> channelToString(c.getId().getChannel()))
                .collect(Collectors.toList());

        return AlertEventDto.builder()
                .id(entity.getId())
                .ruleId(entity.getRule() != null ? entity.getRule().getId().toString() : null)
                .ruleName(entity.getRuleName())
                .queryName(entity.getQueryName())
                .severity(entity.getSeverity().name().toLowerCase())
                .status(entity.getStatus().name().toLowerCase())
                .metric(entity.getMetric().name().toLowerCase())
                .operator(entity.getOperator().name().toLowerCase())
                .threshold(entity.getThreshold())
                .observedValue(entity.getObservedValue())
                .message(entity.getMessage())
                .triggeredAt(entity.getTriggeredAt())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .resolvedAt(entity.getResolvedAt())
                .snoozedUntil(entity.getSnoozedUntil())
                .deliveredChannels(deliveredChannels)
                .build();
    }

    private AlertChannelConfigDto toChannelDto(AlertRuleChannelEntity entity) {
        return AlertChannelConfigDto.builder()
                .channel(channelToString(entity.getChannel()))
                .enabled(entity.isEnabled())
                .recipient(entity.getRecipient())
                .build();
    }

    /** "inApp" | "email" | "sms" | "whatsapp" -> AlertChannel */
    public static AlertChannel channelFromString(String value) {
        if (value == null) return AlertChannel.IN_APP;
        return switch (value.trim()) {
            case "inApp" -> AlertChannel.IN_APP;
            case "email" -> AlertChannel.EMAIL;
            case "sms" -> AlertChannel.SMS;
            case "whatsapp" -> AlertChannel.WHATSAPP;
            default -> AlertChannel.IN_APP;
        };
    }

    /** AlertChannel -> "inApp" | "email" | "sms" | "whatsapp" */
    public static String channelToString(AlertChannel channel) {
        return switch (channel) {
            case IN_APP -> "inApp";
            case EMAIL -> "email";
            case SMS -> "sms";
            case WHATSAPP -> "whatsapp";
        };
    }
}