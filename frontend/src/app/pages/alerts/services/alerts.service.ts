import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { AlertEventDto, AlertRuleDto } from '@core/api/dtos/alert.dto';
import { AlertMapper } from '@core/api/mappers/alert.mapper';
import { AlertEvent, AlertRule } from '@core/models/types';
import { SseService } from '@core/services/sse.service';

const API_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';

@Injectable({ providedIn: 'root' })
export class AlertsService {
  private alertsSubject = new BehaviorSubject<AlertEvent[]>([]);
  private rulesSubject = new BehaviorSubject<AlertRule[]>([]);

  alerts$: Observable<AlertEvent[]> = this.alertsSubject.asObservable();
  alertRules$: Observable<AlertRule[]> = this.rulesSubject.asObservable();

  constructor(private http: HttpClient, private ngZone: NgZone, private sseService: SseService) {
    this.sseService.events$.subscribe(event => {
      if (event.channel === 'alerts_changed') {
        this.loadAlerts();
        this.loadAlertRules();
      }
    });
  }

  
  loadAlerts(): void {
    this.http.get<AlertEventDto[]>(`${API_URL}/alerts`).subscribe({
      next: (list) => {
        const mapped = (list || []).map((dto) => AlertMapper.eventToDomain(dto));
        this.ngZone.run(() => this.alertsSubject.next(mapped));
      },
      error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  loadAlertRules(): void {
    this.http.get<AlertRuleDto[]>(`${API_URL}/alert-rules`).subscribe({
      next: (list) => {
        const mapped = (list || []).map((dto) => AlertMapper.ruleToDomain(dto));
        this.ngZone.run(() => this.rulesSubject.next(mapped));
      },
     error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  upsertAlertRule(rule: AlertRule): void {
    const body = AlertMapper.ruleToUpsertDto(rule);
    const isExisting = this.rulesSubject.value.some((existing) => existing.id === rule.id);
    const request = isExisting
      ? this.http.put<AlertRuleDto>(`${API_URL}/alert-rules/${rule.id}`, body)
      : this.http.post<AlertRuleDto>(`${API_URL}/alert-rules`, body);
    request.subscribe({
      next: () => this.loadAlertRules(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  deleteAlertRule(id: string): void {
    this.http.delete(`${API_URL}/alert-rules/${id}`).subscribe({
      next: () => this.loadAlertRules(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }


  toggleAlertRule(id: string): void {
    this.http.patch(`${API_URL}/alert-rules/${id}/toggle`, {}).subscribe({
      next: () => this.loadAlertRules(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  acknowledgeAlert(id: string): void {
    this.http.patch(`${API_URL}/alerts/${id}/acknowledge`, {}).subscribe({
      next: () => this.loadAlerts(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  snoozeAlert(id: string, minutes: number): void {
    this.http.patch(`${API_URL}/alerts/${id}/snooze`, { minutes }).subscribe({
      next: () => this.loadAlerts(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }

  
  evaluateAlerts(): void {
    this.http.post(`${API_URL}/alerts/evaluate`, {}).subscribe({
      next: () => this.loadAlerts(),
      error: (err) => console.error('[AlertsService]', err)
    });
  }
}