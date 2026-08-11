import {
  AnalyticsCountersDto,
  AnalyticsDailyPointDto,
  AnalyticsEventDto,
  AnalyticsOverviewDto
} from '../dtos/analytics.dto';
import {
  AnalyticsAction,
  AnalyticsCounters,
  AnalyticsDailyPoint,
  AnalyticsEvent,
  AnalyticsTarget
} from '@core/models/types';

export class AnalyticsMapper {
  static countersToDomain(dto: AnalyticsCountersDto): AnalyticsCounters {
    return {
      dashboardViews: dto.dashboardViews || {},
      widgetRawDataViews: dto.widgetRawDataViews || {},
      widgetInteractions: dto.widgetInteractions || {},
      queryExecutions: dto.queryExecutions || {},
      impressions: dto.impressions || 0,
      clicks: dto.clicks || 0
    };
  }

  static dailyToDomain(dto: AnalyticsDailyPointDto): AnalyticsDailyPoint {
    return {
      date: dto.date,
      activeUsers: dto.activeUsers || 0,
      dashboardViews: dto.dashboardViews || 0,
      rawDataViews: dto.rawDataViews || 0,
      clicks: dto.clicks || 0
    };
  }

  static eventToDomain(dto: AnalyticsEventDto): AnalyticsEvent {
    return {
      id: dto.id,
      userId: dto.userId,
      userName: dto.userName,
      action: dto.action as AnalyticsAction,
      target: dto.target as AnalyticsTarget,
      targetId: dto.targetId,
      targetName: dto.targetName,
      timestamp: dto.timestamp,
      dashboardId: dto.dashboardId,
      dashboardName: dto.dashboardName
    };
  }

  static overviewToDomain(dto: AnalyticsOverviewDto): {
    counters: AnalyticsCounters;
    daily: AnalyticsDailyPoint[];
    events: AnalyticsEvent[];
  } {
    return {
      counters: this.countersToDomain(dto.counters),
      daily: (dto.daily || []).map((point) => this.dailyToDomain(point)),
      events: (dto.events || []).map((event) => this.eventToDomain(event))
    };
  }
}