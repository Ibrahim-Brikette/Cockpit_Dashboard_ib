export interface GlobalFilterDto {
  id: string;
  name: string;
  label: string;
  input: string;
  options?: string[];
  fieldId?: string;
  valueMap?: Record<string, string | string[]>;
  defaultValue: string;
  readerVisible: boolean;
  field?: string;
  operator?: string;
  value?: any;
  enabled?: boolean;
}
export interface WidgetFilterDto {
  id: string;
  label?: string;
  fieldId?: string;
  field?: string;
  operator: string;
  value: any;
}
export interface WidgetLayoutDto {
  x: number;
  y: number;
  w: number;
  h: number;
}
export interface WidgetDto {
  id: string;
  type: string;
  title: string;
  queryId?: string;
  layout: WidgetLayoutDto;
  config: any;
  filters?: WidgetFilterDto[];
}
export interface DashboardResponseDto {
  id: string;
  name: string;
  description?: string;
  color?: string;
  status: string;
  ownerId?: string;
  owner?: string;
  shareLevel?: string;
  columns?: number;
  density?: string;
  refreshInterval?: string;
  widgets: WidgetDto[];
  globalFilters?: GlobalFilterDto[];
  tags?: string[];
  favorite?: boolean;
  archived?: boolean;
  createdAt: string;
  updatedAt: string;
  sharedWithMe?: boolean;
}
export interface DashboardRequestDto extends DashboardResponseDto {}
