export type FieldType = 'text' | 'number' | 'date' | 'amount' | 'percent';
export interface DataField {
  id: string;
  key?: string;
  label: string;
  type: FieldType;
  description?: string;
}
export interface DataSource {
  id: string;
  label: string;
  description: string;
  app: HostApp;
  fields: DataField[];
}
export type QueryJoinType = 'inner' | 'left' | 'right' | 'full';
export interface QueryJoin {
  id: string;
  leftSourceId: string;
  leftFieldId: string;
  type: QueryJoinType;
  rightSourceId: string;
  rightFieldId: string;
}
export type QueryTransformationType =
  | 'rename'
  | 'calculated'
  | 'format'
  | 'replaceEmpty'
  | 'filterRows';
export interface QueryTransformation {
  id: string;
  type: QueryTransformationType;
  fieldId?: string;
  outputLabel?: string;
  formula?: string;
  format?: 'year' | 'quarter' | 'month';
  formatPattern?: string;
  replacementValue?: string;
  defaultValue?: string;
  operator?: FilterOperator;
  filterOperator?: FilterOperator;
  value?: string;
  filterValue?: string;
}
export type HostApp = 'ProgesCode' | 'HealthPilot' | 'Care at Home';
export type FilterOperator =
  | 'eq'
  | 'neq'
  | 'gt'
  | 'lt'
  | 'contains'
  | 'in'
  | 'between';
export interface QueryCondition {
  id: string;
  fieldId: string;
  operator: FilterOperator;
  value: string;
  logical: 'AND' | 'OR';
  parametrable: boolean;
}
export type Aggregation = 'none' | 'sum' | 'avg' | 'min' | 'max' | 'count';
export interface QuerySort {
  fieldId: string;
  direction: 'asc' | 'desc';
}
export type QueryVisibility = 'personal' | 'shared';
export interface DataQuery {
  id: string;
  ownerId?: string;
  name: string;
  description: string;
  visibility: QueryVisibility;
  sourceIds: string[];
  sourceId?: string;
  joins: QueryJoin[];
  selectedFieldIds: string[];
  conditions: QueryCondition[];
  transformations: QueryTransformation[];
  groupByFieldIds: string[];
  aggregation: Aggregation;
  aggregationFieldId?: string;
  sort?: QuerySort;
  rowLimit: number;
  usedByWidgets: number;
  updatedAt: string;
}
export type WidgetType =
  | 'kpi'
  | 'bar'
  | 'stacked'
  | 'line'
  | 'pie'
  | 'donut'
  | 'heatmap'
  | 'gauge'
  | 'datagrid'
  | 'text';
export interface WidgetLayout {
  x: number;
  y: number;
  w: number;
  h: number;
}
export type DataGridDensity = 'compact' | 'normal' | 'comfortable';
export interface DataGridConfig {
  visibleColumns?: string[];
  rowsPerPage?: number;
  density?: DataGridDensity;
  showToolbar?: boolean;
  showSearch?: boolean;
  showPagination?: boolean;
  showTotals?: boolean;
  sortable?: boolean;
  filterable?: boolean;
}
export interface WidgetFilter {
  id: string;
  label?: string;
  fieldId?: string;
  operator: FilterOperator;
  value: string;
}
export interface Widget {
  id: string;
  type: WidgetType;
  title: string;
  showTitle: boolean;
  description?: string;
  queryId?: string;
  filters?: WidgetFilter[];
  layout: WidgetLayout;
  refreshInterval: RefreshInterval | 'inherit';
  navigateToDashboardId?: string;
  kpiFormat?: 'amount' | 'percent' | 'integer';
  datagrid?: DataGridConfig;
  text?: string;
}
export type RefreshInterval =
  | 'off'
  | '5s'
  | '10s'
  | '30s'
  | '1min'
  | '5min'
  | '15min'
  | '30min'
  | '1h';
export type GlobalFilterInput =
  | 'select'
  | 'date'
  | 'daterange'
  | 'text'
  | 'multiselect';
export interface GlobalFilter {
  id: string;
  name: string;
  label: string;
  input: GlobalFilterInput;
  options?: string[];
  fieldId?: string;
  valueMap?: Record<string, string | string[]>;
  defaultValue: string;
  readerVisible: boolean;
}
export type DashboardStatus = 'draft' | 'published';
export type ShareLevel = 'private' | 'users' | 'group' | 'organization';
export interface Dashboard {
  id: string;
  name: string;
  description: string;
  color: string;
  status: DashboardStatus;
  ownerId?: string;
  owner: string;
  shareLevel: ShareLevel;
  columns: number;
  density: 'compact' | 'normal' | 'airy';
  refreshInterval: RefreshInterval;
  widgets: Widget[];
  globalFilters: GlobalFilter[];
  tags: string[];
  favorite: boolean;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
  sharedWithMe?: boolean;
}
export interface Datum {
  [key: string]: string | number;
}
export type SourceRow = Record<string, string | number | null>;


export const DATA_SOURCES: DataSource[] = [];
export const MOCK_SOURCE_ROWS: Record<string, SourceRow[]> = {};
export const MOCK_QUERIES: DataQuery[] = [];
export const QUERY_DATASETS: Record<string, Datum[]> = {};
export const MOCK_DASHBOARDS: Dashboard[] = [];
export const DASHBOARD_TEMPLATES: any[] = [];

export type AlertSeverity = 'critical' | 'warning' | 'info';
export type AlertStatus = 'active' | 'acknowledged' | 'snoozed' | 'resolved';
export type AlertMetric = 'total' | 'average' | 'maximum' | 'minimum';
export type AlertComparisonOperator = 'gt' | 'lt' | 'eq';
export type AlertChannel = 'inApp' | 'email' | 'sms' | 'whatsapp';

export interface AlertChannelConfig {
  channel: AlertChannel;
  enabled: boolean;
  recipient?: string;
}

export interface AlertRule {
  id: string;
  name: string;
  queryId: string;
  metric: AlertMetric;
  operator: AlertComparisonOperator;
  threshold: number;
  severity: AlertSeverity;
  enabled: boolean;
  channels: AlertChannelConfig[];
  createdAt: string;
  updatedAt: string;
}

export interface AlertEvent {
  id: string;
  ruleId: string;
  ruleName: string;
  queryName: string;
  severity: AlertSeverity;
  status: AlertStatus;
  metric: AlertMetric;
  operator: AlertComparisonOperator;
  threshold: number;
  observedValue: number;
  message: string;
  triggeredAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  snoozedUntil?: string;
  deliveredChannels: AlertChannel[];
}


export type AnalyticsAction =
  | 'dashboard_view'
  | 'dashboard_click'
  | 'dashboard_impression'
  | 'widget_impression'
  | 'widget_interaction'
  | 'raw_data_view'
  | 'raw_data_export'
  | 'query_execution';

export type AnalyticsTarget = 'dashboard' | 'widget' | 'query';

export interface AnalyticsEvent {
  id: string;
  userId: string;
  userName: string;
  action: AnalyticsAction;
  target: AnalyticsTarget;
  targetId: string;
  targetName: string;
  timestamp: string;
  dashboardId?: string;
  dashboardName?: string;
}

export interface AnalyticsCounters {
  dashboardViews: Record<string, number>;
  widgetRawDataViews: Record<string, number>;
  widgetInteractions: Record<string, number>;
  queryExecutions: Record<string, number>;
  impressions: number;
  clicks: number;
}

export interface AnalyticsDailyPoint {
  date: string;
  activeUsers: number;
  dashboardViews: number;
  rawDataViews: number;
  clicks: number;
}

