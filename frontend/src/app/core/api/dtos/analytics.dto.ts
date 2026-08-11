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