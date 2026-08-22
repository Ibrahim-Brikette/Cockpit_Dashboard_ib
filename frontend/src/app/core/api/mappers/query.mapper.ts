import { QueryResponseDto, QueryRequestDto, QueryJoinDto, QueryConditionDto, QueryTransformationDto, QuerySortDto } from '../dtos/query.dto';
import { DataQuery, QueryJoin, QueryCondition, QueryTransformation, QuerySort } from '@core/models/types';
export class QueryMapper {
  static toDomain(dto: QueryResponseDto): DataQuery {
    return {
      id: String(dto.id),
      ownerId: dto.ownerId,
      name: dto.name,
      description: dto.description || '',
      visibility: dto.visibility ? (dto.visibility.toLowerCase() as any) : 'personal',
      sourceIds: dto.sourceIds || [],
      joins: (dto.joins || []).map(j => this.toJoinDomain(j)),
      selectedFieldIds: dto.selectedFieldIds || [],
      conditions: (dto.conditions || []).map(c => this.toConditionDomain(c)),
      groupByFieldIds: dto.groupByFieldIds || [],
      aggregation: dto.aggregation ? (dto.aggregation.toLowerCase() as any) : 'none',
      aggregationFieldId: dto.aggregationFieldId,
      sort: dto.sort ? this.toSortDomain(dto.sort) : (dto.sorts && dto.sorts.length > 0 ? this.toSortDomain(dto.sorts[0]) : undefined),
      transformations: (dto.transformations || []).map(t => this.toTransformationDomain(t)),
      rowLimit: dto.rowLimit || 1000,
      usedByWidgets: dto.usedByWidgets || 0,
      updatedAt: dto.updatedAt ? String(dto.updatedAt) : new Date().toISOString()
    };
  }
  static toDto(domain: DataQuery): QueryRequestDto {
    return {
      id: domain.id,
      name: domain.name,
      description: domain.description,
      visibility: domain.visibility ? domain.visibility.toUpperCase() : 'PERSONAL',
      sourceIds: domain.sourceIds || [],
      joins: (domain.joins || []).map(j => this.toJoinDto(j)),
      selectedFieldIds: domain.selectedFieldIds || [],
      conditions: (domain.conditions || []).map(c => this.toConditionDto(c)),
      groupByFieldIds: domain.groupByFieldIds || [],
      aggregation: domain.aggregation ? domain.aggregation.toUpperCase() : 'NONE',
      aggregationFieldId: domain.aggregationFieldId,
      sort: domain.sort ? this.toSortDto(domain.sort) : undefined,
      sorts: domain.sort ? [this.toSortDto(domain.sort)] : [],
      transformations: (domain.transformations || []).map(t => this.toTransformationDto(t)),
      rowLimit: domain.rowLimit || 1000,
      tags: [],
      usedByWidgets: domain.usedByWidgets,
      createdAt: domain.updatedAt,
      updatedAt: domain.updatedAt
    };
  }
  private static toJoinDomain(dto: QueryJoinDto): QueryJoin {
    return { id: dto.id, type: dto.type as any, leftSourceId: dto.leftSourceId, leftFieldId: dto.leftFieldId, rightSourceId: dto.rightSourceId, rightFieldId: dto.rightFieldId };
  }
  private static toJoinDto(domain: QueryJoin): QueryJoinDto {
    return { id: domain.id, type: domain.type, leftSourceId: domain.leftSourceId, leftFieldId: domain.leftFieldId, rightSourceId: domain.rightSourceId, rightFieldId: domain.rightFieldId };
  }
  private static toConditionDomain(dto: QueryConditionDto): QueryCondition {
    return { id: dto.id, fieldId: dto.fieldId, operator: dto.operator as any, value: dto.value, logical: (dto.logicalOperator as any) || 'AND', parametrable: false };
  }
  private static toConditionDto(domain: QueryCondition): QueryConditionDto {
    return { id: domain.id, fieldId: domain.fieldId, operator: domain.operator, value: domain.value, logicalOperator: domain.logical };
  }
  private static toSortDomain(dto: QuerySortDto): QuerySort {
    return { fieldId: dto.fieldId, direction: dto.direction as any };
  }
  private static toSortDto(domain: QuerySort): QuerySortDto {
    return { fieldId: domain.fieldId, direction: domain.direction };
  }
  private static toTransformationDomain(dto: QueryTransformationDto): QueryTransformation {
    return {
      id: dto.id,
      type: dto.type as any,
      fieldId: dto.fieldId,
      outputLabel: dto.outputLabel,
      formula: dto.formula,
      format: dto.format as any,
      replacementValue: dto.replacementValue,
      operator: dto.operator as any,
      value: dto.value
    };
  }
  private static toTransformationDto(domain: QueryTransformation): QueryTransformationDto {
    return {
      id: domain.id,
      type: domain.type,
      fieldId: domain.fieldId,
      outputLabel: domain.outputLabel,
      formula: domain.formula,
      format: domain.format,
      replacementValue: domain.replacementValue,
      operator: domain.operator,
      value: domain.value
    };
  }
}
