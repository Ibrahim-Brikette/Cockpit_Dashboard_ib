import {
  AnalyticsAction,
  AnalyticsCounters,
  AnalyticsDailyPoint,
  Dashboard,
  DataQuery
} from '@core/models/types';

export interface RankedMetric {
  id: string;
  label: string;
  value: number;
}

export function topDashboardMetrics(dashboards: Dashboard[], counters: AnalyticsCounters, limit = 5): RankedMetric[] {
  return Object.entries(counters.dashboardViews)
    .map(([id, views]) => ({
      id,
      label: dashboards.find((dashboard) => dashboard.id === id)?.name ?? 'Tableau supprimé',
      value: views
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, limit);
}

export function topQueryMetrics(queries: DataQuery[], counters: AnalyticsCounters, limit = 5): RankedMetric[] {
  return Object.entries(counters.queryExecutions)
    .map(([id, executions]) => ({
      id,
      label: queries.find((query) => query.id === id)?.name ?? 'Requête supprimée',
      value: executions
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, limit);
}

export function engagementRate(counters: AnalyticsCounters): number {
  return counters.impressions ? (counters.clicks / counters.impressions) * 100 : 0;
}

export interface ActivitySeriesPoint extends AnalyticsDailyPoint {
  label: string;
}

export function activitySeries(points: AnalyticsDailyPoint[]): ActivitySeriesPoint[] {
  return points.map((point) => ({
    ...point,
    label: new Date(`${point.date}T12:00:00`).toLocaleDateString('fr-TN', { weekday: 'short' })
  }));
}

const ACTION_LABELS: Record<AnalyticsAction, string> = {
  dashboard_view: 'Consultation de tableau',
  dashboard_click: 'Clic sur tableau',
  dashboard_impression: 'Impression de tableau',
  widget_impression: 'Impression de widget',
  widget_interaction: 'Interaction widget',
  raw_data_view: 'Consultation données brutes',
  raw_data_export: 'Export données brutes',
  query_execution: 'Exécution de requête'
};

export function actionLabel(action: AnalyticsAction): string {
  return ACTION_LABELS[action];
}