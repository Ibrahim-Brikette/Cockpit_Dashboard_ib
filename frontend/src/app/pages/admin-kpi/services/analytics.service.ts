import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { AnalyticsOverviewDto } from '@core/api/dtos/analytics.dto';
import { AnalyticsMapper } from '@core/api/mappers/analytics.mapper';
import { AnalyticsCounters, AnalyticsDailyPoint, AnalyticsEvent } from '@core/models/types';

const API_URL = (typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '4200')
  ? 'http://localhost:8080/api'
  : '/api';

export interface AnalyticsOverview {
  counters: AnalyticsCounters;
  daily: AnalyticsDailyPoint[];
  events: AnalyticsEvent[];
}

const EMPTY_OVERVIEW: AnalyticsOverview = {
  counters: {
    dashboardViews: {},
    widgetRawDataViews: {},
    widgetInteractions: {},
    queryExecutions: {},
    impressions: 0,
    clicks: 0
  },
  daily: [],
  events: []
};

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private overviewSubject = new BehaviorSubject<AnalyticsOverview>(EMPTY_OVERVIEW);
  overview$: Observable<AnalyticsOverview> = this.overviewSubject.asObservable();

  constructor(private http: HttpClient, private ngZone: NgZone) {}

  loadFromBackend(): void {
    this.http.get<AnalyticsOverviewDto>(`${API_URL}/analytics/overview`).subscribe({
      next: (dto) => {
        const overview = AnalyticsMapper.overviewToDomain(dto);
        this.ngZone.run(() => this.overviewSubject.next(overview));
      },
      error: (err) => console.error('[AnalyticsService] échec /analytics/overview', err)
    });
  }
  /**
   * Enregistre un évènement d'usage (vue de dashboard, clic, exécution de requête...).
   */
  track(
    action: 'dashboard_view' | 'dashboard_click' | 'dashboard_impression' | 'widget_impression' |
      'widget_interaction' | 'raw_data_view' | 'raw_data_export' | 'query_execution',
    target: 'dashboard' | 'widget' | 'query',
    targetId: string,
    targetName: string,
    context?: { dashboardId?: string; dashboardName?: string }
  ): void {
    this.http.post(`${API_URL}/analytics/events`, {
      action,
      target,
      targetId,
      targetName,
      dashboardId: context?.dashboardId,
      dashboardName: context?.dashboardName
    }).subscribe({
      error: (err) => console.error('[AnalyticsService] échec envoi événement', action, err)
    });
  }
}