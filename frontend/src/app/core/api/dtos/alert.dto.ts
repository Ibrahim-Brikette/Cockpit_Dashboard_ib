export interface AlertChannelConfigDto {
  channel: 'inApp' | 'email' | 'sms' | 'whatsapp';
  enabled: boolean;
  recipient?: string;
}

export interface AlertRuleDto {
  id: string;
  name: string;
  queryId: string;
  metric: 'total' | 'average' | 'maximum' | 'minimum';
  operator: 'gt' | 'lt' | 'eq';
  threshold: number;
  severity: 'critical' | 'warning' | 'info';
  enabled: boolean;
  channels: AlertChannelConfigDto[];
  createdAt: string;
  updatedAt: string;
}


export interface UpsertAlertRuleRequestDto {
  name: string;
  queryId: string;
  metric: 'total' | 'average' | 'maximum' | 'minimum';
  operator: 'gt' | 'lt' | 'eq';
  threshold: number;
  severity: 'critical' | 'warning' | 'info';
  enabled: boolean;
  channels: AlertChannelConfigDto[];
}

export interface AlertEventDto {
  id: string;
  ruleId: string;
  ruleName: string;
  queryName: string;
  severity: 'critical' | 'warning' | 'info';
  status: 'active' | 'acknowledged' | 'snoozed' | 'resolved';
  metric: 'total' | 'average' | 'maximum' | 'minimum';
  operator: 'gt' | 'lt' | 'eq';
  threshold: number;
  observedValue: number;
  message: string;
  triggeredAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  snoozedUntil?: string;
  deliveredChannels: ('inApp' | 'email' | 'sms' | 'whatsapp')[];
}


export interface SnoozeAlertRequestDto {
  minutes: number;
}