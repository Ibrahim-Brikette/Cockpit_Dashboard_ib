import { AlertEventDto, AlertRuleDto, UpsertAlertRuleRequestDto } from '../dtos/alert.dto';
import { AlertEvent, AlertRule } from '@core/models/types';

export class AlertMapper {
  static ruleToDomain(dto: AlertRuleDto): AlertRule {
    return {
      id: dto.id,
      name: dto.name,
      queryId: dto.queryId,
      metric: dto.metric,
      operator: dto.operator,
      threshold: dto.threshold,
      severity: dto.severity,
      enabled: dto.enabled,
      channels: dto.channels,
      createdAt: dto.createdAt,
      updatedAt: dto.updatedAt
    };
  }

  static ruleToUpsertDto(rule: AlertRule): UpsertAlertRuleRequestDto {
    return {
      name: rule.name,
      queryId: rule.queryId,
      metric: rule.metric,
      operator: rule.operator,
      threshold: rule.threshold,
      severity: rule.severity,
      enabled: rule.enabled,
      channels: rule.channels
    };
  }

  static eventToDomain(dto: AlertEventDto): AlertEvent {
    return {
      id: dto.id,
      ruleId: dto.ruleId,
      ruleName: dto.ruleName,
      queryName: dto.queryName,
      severity: dto.severity,
      status: dto.status,
      metric: dto.metric,
      operator: dto.operator,
      threshold: dto.threshold,
      observedValue: dto.observedValue,
      message: dto.message,
      triggeredAt: dto.triggeredAt,
      acknowledgedAt: dto.acknowledgedAt,
      resolvedAt: dto.resolvedAt,
      snoozedUntil: dto.snoozedUntil,
      deliveredChannels: dto.deliveredChannels
    };
  }
}