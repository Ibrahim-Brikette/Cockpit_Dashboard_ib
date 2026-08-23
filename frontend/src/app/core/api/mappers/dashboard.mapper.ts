import { DashboardResponseDto, DashboardRequestDto, WidgetDto, WidgetLayoutDto, WidgetFilterDto, GlobalFilterDto } from '../dtos/dashboard.dto';
import { Dashboard, Widget, WidgetLayout, WidgetFilter, GlobalFilter } from '@core/models/types';
export class DashboardMapper {
  static toDomain(dto: DashboardResponseDto): Dashboard {
    return {
      id: dto.id,
      name: dto.name,
      description: dto.description || '',
      color: dto.color || '',
      status: dto.status as any,
      ownerId: dto.ownerId,
      owner: dto.owner || '',
      shareLevel: dto.shareLevel as any,
      columns: dto.columns || 12,
      density: dto.density as any,
      refreshInterval: dto.refreshInterval as any,
      widgets: dto.widgets.map(w => this.toWidgetDomain(w)),
      globalFilters: dto.globalFilters?.map(f => this.toGlobalFilterDomain(f)) || [],
      tags: dto.tags || [],
      favorite: dto.favorite || false,
      archived: dto.archived || false,
      createdAt: dto.createdAt,
      updatedAt: dto.updatedAt,
      sharedWithMe: dto.sharedWithMe
    };
  }
  static toDto(domain: Dashboard): DashboardRequestDto {
    return {
      id: domain.id,
      name: domain.name,
      description: domain.description,
      color: domain.color,
      status: domain.status,
      ownerId: domain.ownerId,
      owner: domain.owner,
      shareLevel: domain.shareLevel,
      columns: domain.columns,
      density: domain.density,
      refreshInterval: domain.refreshInterval,
      widgets: domain.widgets.map(w => this.toWidgetDto(w)),
      globalFilters: domain.globalFilters?.map(f => this.toGlobalFilterDto(f)),
      tags: domain.tags,
      favorite: domain.favorite,
      archived: domain.archived,
      createdAt: domain.createdAt,
      updatedAt: domain.updatedAt,
      sharedWithMe: domain.sharedWithMe
    };
  }
  private static toWidgetDomain(dto: WidgetDto): Widget {
    return {
      id: dto.id,
      type: dto.type as any,
      title: dto.title,
      showTitle: true,
      queryId: dto.queryId,
      layout: this.toWidgetLayoutDomain(dto.layout),
      filters: dto.filters?.map(f => this.toWidgetFilterDomain(f)),
      refreshInterval: 'inherit',
      datagrid: dto.config?.datagrid,
      kpiFormat: dto.config?.kpiFormat,
      text: dto.config?.text,
      navigateToDashboardId: dto.config?.navigateToDashboardId
    };
  }
  private static toWidgetDto(domain: Widget): WidgetDto {
    return {
      id: domain.id,
      type: domain.type,
      title: domain.title,
      queryId: domain.queryId,
      layout: this.toWidgetLayoutDto(domain.layout),
      config: {
        datagrid: domain.datagrid,
        kpiFormat: domain.kpiFormat,
        text: domain.text,
        navigateToDashboardId: domain.navigateToDashboardId
      },
      filters: domain.filters?.map(f => this.toWidgetFilterDto(f))
    };
  }
  private static toWidgetLayoutDomain(dto: WidgetLayoutDto): WidgetLayout {
    return { x: dto.x, y: dto.y, w: dto.w, h: dto.h };
  }
  private static toWidgetLayoutDto(domain: WidgetLayout): WidgetLayoutDto {
    return { x: domain.x, y: domain.y, w: domain.w, h: domain.h };
  }
  private static toWidgetFilterDomain(dto: WidgetFilterDto): WidgetFilter {
    return {
      id: dto.id,
      label: dto.label,
      fieldId: dto.fieldId || dto.field,
      operator: dto.operator as any,
      value: dto.value
    };
  }
  private static toWidgetFilterDto(domain: WidgetFilter): WidgetFilterDto {
    return {
      id: domain.id,
      label: domain.label,
      fieldId: domain.fieldId,
      field: domain.fieldId,
      operator: domain.operator,
      value: domain.value
    };
  }
  private static toGlobalFilterDomain(dto: GlobalFilterDto): GlobalFilter {
    return {
      id: dto.id,
      name: dto.name || '',
      label: dto.label || '',
      input: (dto.input as any) || 'text',
      options: dto.options,
      fieldId: dto.fieldId || dto.field,
      valueMap: dto.valueMap,
      defaultValue: dto.defaultValue || '',
      readerVisible: dto.readerVisible !== undefined ? dto.readerVisible : true
    };
  }
  private static toGlobalFilterDto(domain: GlobalFilter): GlobalFilterDto {
    return {
      id: domain.id,
      name: domain.name,
      label: domain.label,
      input: domain.input,
      options: domain.options,
      fieldId: domain.fieldId,
      valueMap: domain.valueMap,
      defaultValue: domain.defaultValue,
      readerVisible: domain.readerVisible,
      field: domain.fieldId,
      operator: 'eq',
      value: domain.defaultValue,
      enabled: true
    };
  }
}
