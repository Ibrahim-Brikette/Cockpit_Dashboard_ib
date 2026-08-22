export interface QueryJoinDto {
  id: string;
  type: string;
  leftSourceId: string;
  leftFieldId: string;
  rightSourceId: string;
  rightFieldId: string;
}
export interface QueryConditionDto {
  id: string;
  fieldId: string;
  operator: string;
  value: any;
  logicalOperator?: string;
}
export interface QueryTransformationDto {
  id: string;
  type: string;
  fieldId?: string;
  outputLabel?: string;
  formula?: string;
  format?: string;
  replacementValue?: string;
  operator?: string;
  value?: string;
}
export interface QuerySortDto {
  fieldId: string;
  direction: string;
}
export interface QueryResponseDto {
  id: string;
  ownerId?: string;
  name: string;
  description?: string;
  visibility?: string;
  sourceIds: string[];
  joins: QueryJoinDto[];
  selectedFieldIds: string[];
  conditions: QueryConditionDto[];
  groupByFieldIds: string[];
  aggregation?: string;
  aggregationFieldId?: string;
  sort?: QuerySortDto;
  sorts?: QuerySortDto[];
  transformations: QueryTransformationDto[];
  rowLimit?: number;
  tags?: string[];
  usedByWidgets?: number;
  createdAt?: string;
  updatedAt?: string;
}
export interface QueryRequestDto extends QueryResponseDto {}
