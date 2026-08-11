import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, combineLatest } from 'rxjs';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { QueryService } from '@pages/query/services/query.service';
import { AlertsService } from '@pages/alerts/services/alerts.service';
import { AlertEvent, AlertRule, AlertStatus, DataQuery } from '@core/models/types';
import { formatMetric, metricLabel, operatorLabel } from '@pages/alerts/utils/alert.utils';
import { AlertEventRowComponent } from './components/alert-event-row.component';
import { AlertSeverityBadgeComponent } from './components/alert-severity-badge.component';
import { AlertRuleModalComponent } from './components/alert-rule-modal.component';
import { ConfirmModalComponent } from '@shared/components/ui/confirm-modal.component';

type FeedFilter = 'all' | AlertStatus;

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [
    CommonModule,
    SvgIconComponent,
    ButtonComponent,
    AlertEventRowComponent,
    AlertSeverityBadgeComponent,
    AlertRuleModalComponent,
    ConfirmModalComponent
  ],
  templateUrl: './alerts.component.html'
})
export class AlertsComponent implements OnInit, OnDestroy {
  readonly filters: { value: FeedFilter; label: string }[] = [
    { value: 'all', label: 'Toutes' },
    { value: 'active', label: 'À traiter' },
    { value: 'acknowledged', label: 'Accusées' },
    { value: 'snoozed', label: 'Reportées' },
    { value: 'resolved', label: 'Résolues' }
  ];

  filter: FeedFilter = 'all';
  alerts: AlertEvent[] = [];
  alertRules: AlertRule[] = [];
  queries: DataQuery[] = [];

  editorOpen = false;
  editorRule?: AlertRule;
  menuRuleId: string | null = null;

  deleteConfirmOpen = false;
  ruleToDeleteId: string | null = null;

  private subscription?: Subscription;

  constructor(
    private queryService: QueryService, 
    private alertsService: AlertsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.queryService.loadFromBackend();
    this.alertsService.loadAlerts();
    this.alertsService.loadAlertRules();

    this.subscription = combineLatest([
      this.queryService.queries$,
      this.alertsService.alerts$,
      this.alertsService.alertRules$
    ]).subscribe(([queries, alerts, alertRules]) => {
      this.queries = queries;
      this.alerts = alerts;
      this.alertRules = alertRules;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  get visibleAlerts(): AlertEvent[] {
    return this.alerts.filter((alert) => this.filter === 'all' || alert.status === this.filter);
  }

  get activeCount(): number {
    return this.alerts.filter((alert) => alert.status === 'active').length;
  }

  get criticalCount(): number {
    return this.alerts.filter((alert) => alert.status === 'active' && alert.severity === 'critical').length;
  }

  get enabledRulesCount(): number {
    return this.alertRules.filter((rule) => rule.enabled).length;
  }

  setFilter(value: FeedFilter): void {
    this.filter = value;
  }

  evaluateNow(): void {
    this.alertsService.evaluateAlerts();
  }

  openCreateRule(): void {
    this.editorRule = undefined;
    this.editorOpen = true;
  }

  openEditRule(rule: AlertRule): void {
    this.editorRule = rule;
    this.editorOpen = true;
    this.menuRuleId = null;
  }

  closeEditor(): void {
    this.editorOpen = false;
  }

  saveRule(rule: AlertRule): void {
    this.alertsService.upsertAlertRule(rule);
  }

  toggleMenu(ruleId: string): void {
    this.menuRuleId = this.menuRuleId === ruleId ? null : ruleId;
  }

  toggleRule(ruleId: string): void {
    this.alertsService.toggleAlertRule(ruleId);
  }

  deleteRule(rule: AlertRule): void {
    this.ruleToDeleteId = rule.id;
    this.deleteConfirmOpen = true;
    this.menuRuleId = null;
  }

  confirmDeleteRule(): void {
    if (this.ruleToDeleteId) {
      this.alertsService.deleteAlertRule(this.ruleToDeleteId);
      this.ruleToDeleteId = null;
    }
    this.deleteConfirmOpen = false;
  }

  acknowledge(alertId: string): void {
    this.alertsService.acknowledgeAlert(alertId);
  }

  snooze(payload: { id: string; minutes: number }): void {
    this.alertsService.snoozeAlert(payload.id, payload.minutes);
  }

  queryNameFor(rule: AlertRule): string {
    return this.queries.find((query) => query.id === rule.queryId)?.name ?? 'Requête supprimée';
  }

  enabledChannelsLabel(rule: AlertRule): string {
    const names: Record<string, string> = { inApp: 'CockpitNG', email: 'E-mail', sms: 'SMS', whatsapp: 'WhatsApp' };
    const labels = rule.channels.filter((channel) => channel.enabled).map((channel) => names[channel.channel]);
    return labels.length ? labels.join(' · ') : 'Aucun canal';
  }

  metricLabel(rule: AlertRule): string {
    return metricLabel(rule.metric);
  }

  operatorLabel(rule: AlertRule): string {
    return operatorLabel(rule.operator);
  }

  formatMetric(value: number): string {
    return formatMetric(value);
  }
}