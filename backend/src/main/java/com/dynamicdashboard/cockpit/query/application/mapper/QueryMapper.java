package com.dynamicdashboard.cockpit.query.application.mapper;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetRepository;
import com.dynamicdashboard.cockpit.query.application.dto.QueryConditionDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryJoinDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import com.dynamicdashboard.cockpit.query.application.dto.QuerySortDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryTransformationDto;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.QueryConditionRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryGroupByFieldRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryJoinRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySelectedFieldRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySortRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySourceBindingRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryTransformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class QueryMapper {
    private final QuerySourceBindingRepository querySourceBindingRepository;
    private final QuerySelectedFieldRepository querySelectedFieldRepository;
    private final QueryGroupByFieldRepository queryGroupByFieldRepository;
    private final QueryJoinRepository queryJoinRepository;
    private final QueryConditionRepository queryConditionRepository;
    private final QueryTransformationRepository queryTransformationRepository;
    private final QuerySortRepository querySortRepository;
    private final WidgetRepository widgetRepository;
    public QueryResponseDto toDto(DataQueryEntity entity) {
        if (entity == null) return null;
        List<String> sourceIds = querySourceBindingRepository.findByQueryIdOrderByPositionIndexAsc(entity.getId())
                .stream()
                .map(sb -> sb.getDataSource().getSourceKey())
                .collect(Collectors.toList());
        List<String> selectedFieldIds = querySelectedFieldRepository.findByIdQueryIdOrderByPositionIndexAsc(entity.getId())
                .stream()
                .map(sf -> sf.getField().getFieldKey())
                .collect(Collectors.toList());
        List<String> groupByFieldIds = queryGroupByFieldRepository.findByIdQueryIdOrderByPositionIndexAsc(entity.getId())
                .stream()
                .map(gb -> gb.getField().getFieldKey())
                .collect(Collectors.toList());
        List<QueryJoinDto> joins = queryJoinRepository.findByQueryId(entity.getId())
                .stream()
                .map(j -> QueryJoinDto.builder()
                        .id(j.getId().toString())
                        .leftSourceId(j.getLeftSource().getSourceKey())
                        .leftFieldId(j.getLeftField().getFieldKey())
                        .type(j.getJoinType().name().toLowerCase())
                        .rightSourceId(j.getRightSource().getSourceKey())
                        .rightFieldId(j.getRightField().getFieldKey())
                        .build())
                .collect(Collectors.toList());
        List<QueryConditionDto> conditions = queryConditionRepository.findByQueryId(entity.getId())
                .stream()
                .map(c -> QueryConditionDto.builder()
                        .id(c.getId().toString())
                        .fieldId(c.getField().getFieldKey())
                        .operator(c.getOperator().name().toLowerCase())
                        .value(c.getValueExpression())
                        .logical(c.getLogicalOperator().name())
                        .parametrable(c.isParameterizable())
                        .build())
                .collect(Collectors.toList());
        List<QueryTransformationDto> transformations = queryTransformationRepository.findByQueryId(entity.getId())
                .stream()
                .map(t -> QueryTransformationDto.builder()
                        .id(t.getId().toString())
                        .type(t.getTransformationType().name().toLowerCase())
                        .fieldId(t.getTargetField() != null ? t.getTargetField().getId().toString() : null)
                        .outputLabel(t.getOutputLabel())
                        .formula(t.getFormulaExpression())
                        .format(t.getFormatOption())
                        .replacementValue(t.getReplacementValue())
                        .operator(t.getOperator() != null ? t.getOperator().name().toLowerCase() : null)
                        .value(t.getComparisonValue())
                        .build())
                .collect(Collectors.toList());
        QuerySortDto sortDto = querySortRepository.findById(entity.getId())
                .map(s -> QuerySortDto.builder()
                        .fieldId(s.getField().getFieldKey())
                        .direction(s.getDirection().name().toLowerCase())
                        .build())
                .orElse(null);
        int usedByWidgetsCount = widgetRepository.countByQueryId(entity.getId());
        return QueryResponseDto.builder()
                .id(entity.getId())
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .name(entity.getQueryName())
                .description(entity.getQueryDescription())
                .visibility(entity.getVisibility() != null ? entity.getVisibility().name().toLowerCase() : "shared")
                .sourceIds(sourceIds)
                .joins(joins)
                .selectedFieldIds(selectedFieldIds)
                .conditions(conditions)
                .transformations(transformations)
                .groupByFieldIds(groupByFieldIds)
                .aggregation(entity.getAggregation() != null ? entity.getAggregation().name().toLowerCase() : "none")
                .aggregationFieldId(entity.getAggregationField() != null ? entity.getAggregationField().getFieldKey() : null)
                .sort(sortDto)
                .rowLimit(entity.getRowLimit())
                .usedByWidgets(usedByWidgetsCount)
                .updatedAt(entity.getUpdatedAt())
                .tenantId(entity.getTenantId())
                .build();
    }
}
