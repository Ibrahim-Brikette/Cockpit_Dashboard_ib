export interface AnalyticsEventDto {
  id: string;
  userId: string;
  userName: string;
  action: string;
  target: string;
  targetId: string;
  targetName: string;
  timestamp: string;
  dashboardId?: string;
  dashboardName?: string;
}

export interface AnalyticsCountersDto {
  dashboardViews: Record<string, number>;
  widgetRawDataViews: Record<string, number>;
  widgetInteractions: Record<string, number>;
  /** @deprecated Toujours vide depuis la correction du 14/08 : les exécutions de requêtes
   *  sont désormais dans la table d'audit (voir /api/audit-events/top-query-executions),
   *  plus dans analytics_event, pour ne pas compter chaque rendu de widget comme un clic. */
  queryExecutions: Record<string, number>;
  impressions: number;
  clicks: number;
}

export interface AnalyticsDailyPointDto {
  date: string;
  activeUsers: number;
  dashboardViews: number;
  rawDataViews: number;
  clicks: number;
}


export interface AnalyticsOverviewDto {
  counters: AnalyticsCountersDto;
  daily: AnalyticsDailyPointDto[];
  events: AnalyticsEventDto[];
}