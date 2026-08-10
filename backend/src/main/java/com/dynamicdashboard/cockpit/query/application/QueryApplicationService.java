package com.dynamicdashboard.cockpit.query.application;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.catalog.repository.DataFieldRepository;
import com.dynamicdashboard.cockpit.catalog.repository.DataSourceRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.query.application.dto.QueryConditionDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryJoinDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryRequestDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryTransformationDto;
import com.dynamicdashboard.cockpit.query.application.mapper.QueryMapper;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.domain.QueryConditionEntity;
import com.dynamicdashboard.cockpit.query.domain.QueryGroupByFieldEntity;
import com.dynamicdashboard.cockpit.query.domain.QueryGroupByFieldEntity.QueryGroupByFieldId;
import com.dynamicdashboard.cockpit.query.domain.QueryJoinEntity;
import com.dynamicdashboard.cockpit.query.domain.QuerySelectedFieldEntity;
import com.dynamicdashboard.cockpit.query.domain.QuerySelectedFieldEntity.QuerySelectedFieldId;
import com.dynamicdashboard.cockpit.query.domain.QuerySortEntity;
import com.dynamicdashboard.cockpit.query.domain.QuerySourceBindingEntity;
import com.dynamicdashboard.cockpit.query.domain.QueryTransformationEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryConditionRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryGroupByFieldRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryJoinRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySelectedFieldRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySortRepository;
import com.dynamicdashboard.cockpit.query.repository.QuerySourceBindingRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryTransformationRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AggregationType;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FilterOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.LogicalOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryJoinType;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryTransformationType;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryVisibility;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.SortDirection;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import com.dynamicdashboard.cockpit.shared.security.tenant.TenantContext;
import com.dynamicdashboard.cockpit.shared.utils.ParsingUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class QueryApplicationService {
    private final DataQueryRepository dataQueryRepository;
    private final QueryConditionRepository queryConditionRepository;
    private final QueryGroupByFieldRepository queryGroupByFieldRepository;
    private final QueryJoinRepository queryJoinRepository;
    private final QuerySelectedFieldRepository querySelectedFieldRepository;
    private final QuerySortRepository querySortRepository;
    private final QuerySourceBindingRepository querySourceBindingRepository;
    private final QueryTransformationRepository queryTransformationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final DataFieldRepository dataFieldRepository;
    private final CurrentUserService currentUserService;
    private final QueryMapper queryMapper;
    private final com.dynamicdashboard.cockpit.audit.application.AuditApplicationService auditApplicationService;
    private final com.dynamicdashboard.cockpit.dashboard.repository.WidgetRepository widgetRepository;
    private final com.dynamicdashboard.cockpit.datasource.application.VaultSecretService vaultSecretService;
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private QueryApplicationService self;
    private static final java.util.Map<String, com.zaxxer.hikari.HikariDataSource> HIKARI_DATA_SOURCES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, CachedQueryResult> FAST_QUERY_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private record WorkingCredentials(String host, String password) {}
    private record CachedQueryResult(long timestamp, List<Map<String, Object>> data) {}
    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeQueryData(UUID queryId, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto> filters) {
        long now = System.currentTimeMillis();
        String queryCacheKey = queryId.toString() + "_" + (filters != null ? filters.hashCode() : 0);
        CachedQueryResult cached = FAST_QUERY_CACHE.get(queryCacheKey);
        if (cached != null && (now - cached.timestamp()) < 3000) {
            return cached.data();
        }
        DataQueryEntity query = dataQueryRepository.findById(queryId).orElse(null);
        if (query == null) return java.util.Collections.emptyList();
        List<QuerySourceBindingEntity> sources = querySourceBindingRepository.findByQueryIdOrderByPositionIndexAsc(query.getId());
        if (sources.isEmpty()) return java.util.Collections.emptyList();
        String primaryTable = getPhysicalTableName(sources.get(0).getDataSource());
        if (primaryTable == null || primaryTable.isBlank()) return java.util.Collections.emptyList();
        com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity conn = sources.get(0).getDataSource().getDbConnection();
        com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy strategy = com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy.from(conn != null ? conn.getDbType() : null);
        org.jooq.DSLContext dsl = org.jooq.impl.DSL.using(strategy.getDialect());
        List<QueryGroupByFieldEntity> groupByFields = queryGroupByFieldRepository.findByIdQueryIdOrderByPositionIndexAsc(query.getId());
        List<QueryTransformationEntity> transformations = queryTransformationRepository.findByQueryId(query.getId());
        org.jooq.SelectJoinStep<?> jooqQuery;
        org.jooq.Field<?> groupByExpr = null;
        if (query.getAggregation() != null && !AggregationType.NONE.equals(query.getAggregation())) {
            org.jooq.Field<?> groupByColumn = org.jooq.impl.DSL.val("Tous").as("label");
            if (!groupByFields.isEmpty()) {
                DataFieldEntity gbField = groupByFields.get(0).getField();
                org.jooq.Field<?> rawCol = buildJooqField(getFieldColumnName(gbField));
                groupByExpr = applyDateTransformationEntity(rawCol, gbField, transformations);
                groupByColumn = groupByExpr.as("label");
            }
            String aggColumnName = query.getAggregationField() != null
                ? getFieldColumnName(query.getAggregationField())
                : "*";
            DataFieldEntity aggFieldEntity = query.getAggregationField();
            boolean isNumericField = aggFieldEntity != null && 
                (com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.AMOUNT.equals(aggFieldEntity.getFieldType()) || 
                 com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.NUMBER.equals(aggFieldEntity.getFieldType()) ||
                 com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.PERCENT.equals(aggFieldEntity.getFieldType()));
            org.jooq.Field<?> aggColumn;
            if (AggregationType.SUM.equals(query.getAggregation())) {
                if (isNumericField) {
                    aggColumn = org.jooq.impl.DSL.sum(buildJooqField(aggColumnName, java.math.BigDecimal.class)).as("value");
                } else {
                    aggColumn = org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
                }
            } else if (AggregationType.AVG.equals(query.getAggregation())) {
                if (isNumericField) {
                    aggColumn = org.jooq.impl.DSL.avg(buildJooqField(aggColumnName, java.math.BigDecimal.class)).as("value");
                } else {
                    aggColumn = org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
                }
            } else if (AggregationType.COUNT.equals(query.getAggregation())) {
                aggColumn = org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.MAX.equals(query.getAggregation())) {
                aggColumn = org.jooq.impl.DSL.max(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.MIN.equals(query.getAggregation())) {
                aggColumn = org.jooq.impl.DSL.min(buildJooqField(aggColumnName)).as("value");
            } else {
                aggColumn = buildJooqField(aggColumnName).as("value");
            }
            jooqQuery = dsl.select(groupByColumn, aggColumn).from(buildJooqTable(primaryTable));
        } else {
            jooqQuery = dsl.select(org.jooq.impl.DSL.asterisk()).from(buildJooqTable(primaryTable));
        }
        java.util.Set<String> includedTables = new java.util.HashSet<>();
        includedTables.add(primaryTable.toLowerCase());
        List<QueryJoinEntity> joins = queryJoinRepository.findByQueryId(query.getId());
        for (QueryJoinEntity join : joins) {
            if (join.getLeftSource() != null && join.getRightSource() != null && join.getLeftField() != null && join.getRightField() != null) {
                String leftTable = getPhysicalTableName(join.getLeftSource());
                String rightTable = getPhysicalTableName(join.getRightSource());
                String leftField = getFieldColumnName(join.getLeftField());
                String rightField = getFieldColumnName(join.getRightField());
                if (leftTable != null && !leftTable.isBlank() && rightTable != null && !rightTable.isBlank()) {
                    String newTable = null;
                    if (includedTables.contains(leftTable.toLowerCase()) && !includedTables.contains(rightTable.toLowerCase())) {
                        newTable = rightTable;
                    } else if (includedTables.contains(rightTable.toLowerCase()) && !includedTables.contains(leftTable.toLowerCase())) {
                        newTable = leftTable;
                    }
                    if (newTable != null) {
                        org.jooq.Table<?> jooqNewTable = buildJooqTable(newTable);
                        org.jooq.Condition onCondition = buildJooqField(leftField).eq(buildJooqField(rightField));
                        if (QueryJoinType.INNER.equals(join.getJoinType())) {
                            jooqQuery = jooqQuery.join(jooqNewTable).on(onCondition);
                        } else if (QueryJoinType.RIGHT.equals(join.getJoinType())) {
                            jooqQuery = jooqQuery.rightJoin(jooqNewTable).on(onCondition);
                        } else {
                            jooqQuery = jooqQuery.leftJoin(jooqNewTable).on(onCondition);
                        }
                        includedTables.add(newTable.toLowerCase());
                    }
                }
            }
        }
        org.jooq.Condition finalCondition = org.jooq.impl.DSL.noCondition();
        List<QueryConditionEntity> conditions = queryConditionRepository.findByQueryId(query.getId());
        for (QueryConditionEntity condition : conditions) {
            if (condition.getField() != null && condition.getValueExpression() != null && !condition.getValueExpression().isBlank()) {
                String col = getFieldColumnName(condition.getField());
                if (FilterOperator.EQ.equals(condition.getOperator())) {
                    finalCondition = finalCondition.and(buildJooqField(col).eq(condition.getValueExpression()));
                }
            }
        }
        if (filters != null && !filters.isEmpty()) {
            for (com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto filter : filters) {
                if (filter.getFieldId() != null && filter.getValue() != null && !filter.getValue().isBlank() && !"GLOBAL_DATE_RANGE".equals(filter.getFieldId())) {
                    DataFieldEntity field = dataFieldRepository.findByFieldKey(filter.getFieldId()).orElse(null);
                    if (field != null) {
                        String col = getFieldColumnName(field);
                        if ("eq".equals(filter.getOperator())) {
                            finalCondition = finalCondition.and(buildJooqField(col).eq(filter.getValue())); 
                        } else if ("in".equals(filter.getOperator())) {
                            String[] vals = filter.getValue().split(",");
                            List<String> trimmedVals = new java.util.ArrayList<>();
                            for (String v : vals) trimmedVals.add(v.trim());
                            finalCondition = finalCondition.and(buildJooqField(col).in(trimmedVals));
                        }
                    }
                }
            }
        }
        org.jooq.SelectConditionStep<?> whereQuery = jooqQuery.where(finalCondition);
        org.jooq.SelectOrderByStep<?> orderStep = (query.getAggregation() != null && !AggregationType.NONE.equals(query.getAggregation()) && groupByExpr != null)
            ? (org.jooq.SelectOrderByStep<?>) whereQuery.groupBy(groupByExpr)
            : whereQuery;
        org.jooq.SelectLimitStep<?> limitStep = orderStep;
        Optional<QuerySortEntity> sortOpt = querySortRepository.findById(query.getId());
        if (sortOpt.isPresent() && sortOpt.get().getField() != null) {
            DataFieldEntity sortField = sortOpt.get().getField();
            boolean isDesc = SortDirection.DESC.equals(sortOpt.get().getDirection());
            org.jooq.Field<?> sortColExpr;
            boolean sortOnAggValue = query.getAggregationField() != null
                && sortField.getId().equals(query.getAggregationField().getId())
                && query.getAggregation() != null && !AggregationType.NONE.equals(query.getAggregation());
            if (sortOnAggValue) {
                sortColExpr = org.jooq.impl.DSL.field("value");
            } else {
                sortColExpr = buildJooqField(getFieldColumnName(sortField));
                if (groupByExpr != null && !groupByFields.isEmpty() && groupByFields.get(0).getField().getId().equals(sortField.getId())) {
                    sortColExpr = groupByExpr;
                }
            }
            limitStep = (org.jooq.SelectLimitStep<?>) orderStep.orderBy(isDesc ? sortColExpr.desc() : sortColExpr.asc());
        }
        org.jooq.Select<?> finalQuery = limitStep;
        Integer limit = query.getRowLimit();
        if (limit != null && limit > 0) {
            finalQuery = limitStep.limit(limit);
        }
        try {
            String sql = finalQuery.getSQL(org.jooq.conf.ParamType.INLINED);
            if (conn == null) {
                return java.util.Collections.emptyList();
            }
            sql = adjustSqlForDialect(sql, conn.getDbType(), limit);
            String password = vaultSecretService.retrievePassword(conn.getVaultSecretKey());
            if (password == null) password = "";
            String host = conn.getDbHost();
            int port = conn.getDbPort();
            String dbName = conn.getDbName();
            String username = conn.getDbUsername();
            org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).info("Executing generated SQL for queryId {}: {}", queryId, sql);
            try (java.sql.Connection jdbcConn = createResilientConnection(conn != null ? conn.getId() : null, strategy, host, port, dbName, username, password);
                 java.sql.Statement stmt = jdbcConn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(sql)) {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<Map<String, Object>> resultRows = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        if (colName == null || colName.isBlank()) {
                            colName = meta.getColumnName(i);
                        }
                        Object val = rs.getObject(i);
                        row.put(colName, val);
                        if (colName != null && !colName.isBlank()) {
                            row.put(colName.toLowerCase(), val);
                        }
                    }
                    resultRows.add(row);
                }
                org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).info("Successfully retrieved {} rows for queryId {}", resultRows.size(), queryId);
                FAST_QUERY_CACHE.put(queryCacheKey, new CachedQueryResult(now, resultRows));
                return resultRows;
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).error("Query execution error for queryId {}: {}", queryId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> previewDraftQuery(QueryRequestDto dto) {
        if (dto == null || dto.getSourceIds() == null || dto.getSourceIds().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity primaryDs = resolveDataSource(dto.getSourceIds().get(0)).orElse(null);
        if (primaryDs == null) return java.util.Collections.emptyList();
        String primaryTable = getPhysicalTableName(primaryDs);
        if (primaryTable == null || primaryTable.isBlank()) return java.util.Collections.emptyList();
        com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity conn = primaryDs.getDbConnection();
        com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy strategy = com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy.from(conn != null ? conn.getDbType() : null);
        org.jooq.DSLContext dsl = org.jooq.impl.DSL.using(strategy.getDialect());
        AggregationType aggregation = ParsingUtils.parseEnum(AggregationType.class, dto.getAggregation(), AggregationType.NONE);
        org.jooq.SelectJoinStep<?> jooqQuery;
        org.jooq.Field<?> draftGroupByExpr = null;
        if (aggregation != AggregationType.NONE) {
            org.jooq.Field<?> groupByColumn = org.jooq.impl.DSL.val("Tous").as("label");
            if (dto.getGroupByFieldIds() != null && !dto.getGroupByFieldIds().isEmpty()) {
                Optional<DataFieldEntity> gbF = resolveField(dto.getGroupByFieldIds().get(0));
                if (gbF.isPresent()) {
                    org.jooq.Field<?> rawCol = buildJooqField(getFieldColumnName(gbF.get()));
                    draftGroupByExpr = applyDateTransformation(rawCol, gbF.get(), dto.getTransformations());
                    groupByColumn = draftGroupByExpr.as("label");
                }
            }
            String aggColumnName = "*";
            DataFieldEntity aggFieldEntity = null;
            if (dto.getAggregationFieldId() != null && !dto.getAggregationFieldId().isBlank()) {
                aggFieldEntity = resolveField(dto.getAggregationFieldId()).orElse(null);
                if (aggFieldEntity != null) {
                    aggColumnName = getFieldColumnName(aggFieldEntity);
                }
            }
            boolean isNumericField = aggFieldEntity != null && 
                (com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.AMOUNT.equals(aggFieldEntity.getFieldType()) || 
                 com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.NUMBER.equals(aggFieldEntity.getFieldType()) ||
                 com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType.PERCENT.equals(aggFieldEntity.getFieldType()));
            org.jooq.Field<?> aggColumn;
            if (AggregationType.SUM.equals(aggregation)) {
                aggColumn = isNumericField ? org.jooq.impl.DSL.sum(buildJooqField(aggColumnName, java.math.BigDecimal.class)).as("value") : org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.AVG.equals(aggregation)) {
                aggColumn = isNumericField ? org.jooq.impl.DSL.avg(buildJooqField(aggColumnName, java.math.BigDecimal.class)).as("value") : org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.COUNT.equals(aggregation)) {
                aggColumn = org.jooq.impl.DSL.count(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.MAX.equals(aggregation)) {
                aggColumn = org.jooq.impl.DSL.max(buildJooqField(aggColumnName)).as("value");
            } else if (AggregationType.MIN.equals(aggregation)) {
                aggColumn = org.jooq.impl.DSL.min(buildJooqField(aggColumnName)).as("value");
            } else {
                aggColumn = buildJooqField(aggColumnName).as("value");
            }
            jooqQuery = dsl.select(groupByColumn, aggColumn).from(buildJooqTable(primaryTable));
        } else {
            jooqQuery = dsl.select(org.jooq.impl.DSL.asterisk()).from(buildJooqTable(primaryTable));
        }
        java.util.Set<String> includedTables = new java.util.HashSet<>();
        includedTables.add(primaryTable.toLowerCase());
        if (dto.getJoins() != null) {
            for (com.dynamicdashboard.cockpit.query.application.dto.QueryJoinDto joinDto : dto.getJoins()) {
                com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity leftDs = resolveDataSource(joinDto.getLeftSourceId()).orElse(null);
                com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity rightDs = resolveDataSource(joinDto.getRightSourceId()).orElse(null);
                DataFieldEntity leftDf = resolveField(joinDto.getLeftFieldId()).orElse(null);
                DataFieldEntity rightDf = resolveField(joinDto.getRightFieldId()).orElse(null);
                if (leftDs != null && rightDs != null && leftDf != null && rightDf != null) {
                    String leftTable = getPhysicalTableName(leftDs);
                    String rightTable = getPhysicalTableName(rightDs);
                    String leftField = getFieldColumnName(leftDf);
                    String rightField = getFieldColumnName(rightDf);
                    if (leftTable != null && !leftTable.isBlank() && rightTable != null && !rightTable.isBlank()) {
                        String newTable = null;
                        if (includedTables.contains(leftTable.toLowerCase()) && !includedTables.contains(rightTable.toLowerCase())) {
                            newTable = rightTable;
                        } else if (includedTables.contains(rightTable.toLowerCase()) && !includedTables.contains(leftTable.toLowerCase())) {
                            newTable = leftTable;
                        }
                        if (newTable != null) {
                            org.jooq.Table<?> jooqNewTable = buildJooqTable(newTable);
                            org.jooq.Condition onCondition = buildJooqField(leftField).eq(buildJooqField(rightField));
                            QueryJoinType jType = ParsingUtils.parseEnum(QueryJoinType.class, joinDto.getType(), QueryJoinType.INNER);
                            if (QueryJoinType.INNER.equals(jType)) {
                                jooqQuery = jooqQuery.join(jooqNewTable).on(onCondition);
                            } else if (QueryJoinType.RIGHT.equals(jType)) {
                                jooqQuery = jooqQuery.rightJoin(jooqNewTable).on(onCondition);
                            } else {
                                jooqQuery = jooqQuery.leftJoin(jooqNewTable).on(onCondition);
                            }
                            includedTables.add(newTable.toLowerCase());
                        }
                    }
                }
            }
        }
        org.jooq.Condition finalCondition = org.jooq.impl.DSL.noCondition();
        if (dto.getConditions() != null) {
            for (com.dynamicdashboard.cockpit.query.application.dto.QueryConditionDto cDto : dto.getConditions()) {
                if (cDto.getFieldId() != null && cDto.getValue() != null && !cDto.getValue().isBlank()) {
                    DataFieldEntity cField = resolveField(cDto.getFieldId()).orElse(null);
                    if (cField != null) {
                        String col = getFieldColumnName(cField);
                        FilterOperator op = ParsingUtils.parseEnum(FilterOperator.class, cDto.getOperator(), FilterOperator.EQ);
                        switch (op) {
                            case EQ:
                                finalCondition = finalCondition.and(buildJooqField(col).eq(cDto.getValue()));
                                break;
                            case NEQ:
                                finalCondition = finalCondition.and(buildJooqField(col).ne(cDto.getValue()));
                                break;
                            case GT:
                                finalCondition = finalCondition.and(buildJooqField(col, String.class).gt(cDto.getValue()));
                                break;
                            case LT:
                                finalCondition = finalCondition.and(buildJooqField(col, String.class).lt(cDto.getValue()));
                                break;
                            case CONTAINS:
                                finalCondition = finalCondition.and(buildJooqField(col, String.class).containsIgnoreCase(cDto.getValue()));
                                break;
                            case IN:
                                String[] vals = cDto.getValue().split(",");
                                List<String> trimmedVals = new java.util.ArrayList<>();
                                for (String v : vals) trimmedVals.add(v.trim());
                                finalCondition = finalCondition.and(buildJooqField(col).in(trimmedVals));
                                break;
                            default:
                                finalCondition = finalCondition.and(buildJooqField(col).eq(cDto.getValue()));
                                break;
                        }
                    }
                }
            }
        }
        org.jooq.SelectConditionStep<?> whereQuery = jooqQuery.where(finalCondition);
        org.jooq.SelectOrderByStep<?> orderStep = whereQuery;
        if (aggregation != AggregationType.NONE && draftGroupByExpr != null) {
            orderStep = (org.jooq.SelectOrderByStep<?>) whereQuery.groupBy(draftGroupByExpr);
        }
        org.jooq.SelectLimitStep<?> limitStep = orderStep;
        if (dto.getSort() != null && dto.getSort().getFieldId() != null) {
            boolean isAggValueSort = "__agg_value__".equals(dto.getSort().getFieldId());
            if (isAggValueSort && aggregation != AggregationType.NONE) {
                org.jooq.Field<?> aggValField = org.jooq.impl.DSL.field("value");
                boolean isDesc = "desc".equalsIgnoreCase(dto.getSort().getDirection());
                limitStep = (org.jooq.SelectLimitStep<?>) orderStep.orderBy(isDesc ? aggValField.desc() : aggValField.asc());
            } else {
                DataFieldEntity sortField = resolveField(dto.getSort().getFieldId()).orElse(null);
                if (sortField != null) {
                    org.jooq.Field<?> sortColExpr = buildJooqField(getFieldColumnName(sortField));
                    if (draftGroupByExpr != null && dto.getGroupByFieldIds() != null && !dto.getGroupByFieldIds().isEmpty() && dto.getGroupByFieldIds().get(0).equals(dto.getSort().getFieldId())) {
                        sortColExpr = draftGroupByExpr;
                    }
                    if ("desc".equalsIgnoreCase(dto.getSort().getDirection())) {
                        limitStep = (org.jooq.SelectLimitStep<?>) orderStep.orderBy(sortColExpr.desc());
                    } else {
                        limitStep = (org.jooq.SelectLimitStep<?>) orderStep.orderBy(sortColExpr.asc());
                    }
                }
            }
        }
        Integer limit = dto.getRowLimit() != null && dto.getRowLimit() > 0 ? dto.getRowLimit() : 100;
        org.jooq.Select<?> finalQuery = limitStep.limit(limit);
        try {
            String sql = finalQuery.getSQL(org.jooq.conf.ParamType.INLINED);
            if (conn == null) return java.util.Collections.emptyList();
            sql = adjustSqlForDialect(sql, conn.getDbType(), limit);
            String password = vaultSecretService.retrievePassword(conn.getVaultSecretKey());
            if (password == null) password = "";
            String host = conn.getDbHost();
            int port = conn.getDbPort();
            String dbName = conn.getDbName();
            String username = conn.getDbUsername();
            org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).info("Executing DRAFT PREVIEW SQL: {}", sql);
            try (java.sql.Connection jdbcConn = createResilientConnection(conn.getId(), strategy, host, port, dbName, username, password);
                 java.sql.Statement stmt = jdbcConn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(sql)) {
                java.sql.ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<Map<String, Object>> resultRows = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        if (colName == null || colName.isBlank()) colName = meta.getColumnName(i);
                        Object val = rs.getObject(i);
                        row.put(colName, val);
                        if (colName != null && !colName.isBlank()) {
                            row.put(colName.toLowerCase(), val);
                        }
                    }
                    resultRows.add(row);
                }
                return resultRows;
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).error("Draft query preview error: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
    private WorkingCredentials resolveWorkingCredentials(com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy strategy, String host, int port, String dbName, String username, String password) {
        if (host != null && !host.isBlank()) {
            String primaryUrl = strategy.buildUrl(host, port, dbName);
            if (password != null && !password.isBlank()) {
                try (java.sql.Connection c = java.sql.DriverManager.getConnection(primaryUrl, username, password)) {
                    return new WorkingCredentials(host, password);
                } catch (Exception ignored) {}
            }
        }
        List<String> hostsToTry = new java.util.ArrayList<>();
        if (host != null && !host.isBlank() && !"127.0.0.1".equals(host) && !"localhost".equals(host)) {
            hostsToTry.add(host);
        }
        hostsToTry.add("172.17.0.1");
        hostsToTry.add("host.docker.internal");
        hostsToTry.addAll(strategy.getContainerCandidates());
        List<String> passwordsToTry = new java.util.ArrayList<>();
        if (password != null && !password.isBlank()) {
            passwordsToTry.add(password);
        }
        String envPwd = System.getenv("COCKPIT_DB_PASSWORD");
        if (envPwd != null && !envPwd.isBlank() && !passwordsToTry.contains(envPwd)) {
            passwordsToTry.add(envPwd);
        }
        if (!passwordsToTry.contains("postgres")) passwordsToTry.add("postgres");
        if (!passwordsToTry.contains("moezkr")) passwordsToTry.add("moezkr");
        if (!passwordsToTry.contains("cockpit")) passwordsToTry.add("cockpit");
        if (!passwordsToTry.contains("")) passwordsToTry.add("");
        for (String pwd : passwordsToTry) {
            for (String targetHost : hostsToTry) {
                String url = strategy.buildUrl(targetHost, port, dbName);
                try (java.sql.Connection c = java.sql.DriverManager.getConnection(url, username, pwd)) {
                    return new WorkingCredentials(targetHost, pwd);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
    private java.sql.Connection createResilientConnection(UUID connectionId, com.dynamicdashboard.cockpit.shared.utils.DatabaseDriverStrategy strategy, String host, int port, String dbName, String username, String password) throws Exception {
        String cacheKey = (connectionId != null ? connectionId.toString() : (dbName + "_" + username)) + "_" + strategy.name();
        Class.forName(strategy.getDriverClassName());
        com.zaxxer.hikari.HikariDataSource ds = HIKARI_DATA_SOURCES.get(cacheKey);
        if (ds != null && !ds.isClosed()) {
            try {
                return ds.getConnection();
            } catch (Exception ignored) {
                HIKARI_DATA_SOURCES.remove(cacheKey);
                try { ds.close(); } catch (Exception ignored2) {}
            }
        }
        WorkingCredentials creds = resolveWorkingCredentials(strategy, host, port, dbName, username, password);
        if (creds == null) {
            throw new RuntimeException("Could not connect to database on any host candidate for " + dbName);
        }
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setDriverClassName(strategy.getDriverClassName());
        config.setJdbcUrl(strategy.buildUrl(creds.host(), port, dbName));
        config.setUsername(username);
        config.setPassword(creds.password());
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setKeepaliveTime(30000);
        config.setConnectionTimeout(2000);
        config.setPoolName("CockpitPool-" + Math.abs(cacheKey.hashCode() % 10000));
        com.zaxxer.hikari.HikariDataSource newDs = new com.zaxxer.hikari.HikariDataSource(config);
        HIKARI_DATA_SOURCES.put(cacheKey, newDs);
        org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).info("Created HikariCP connection pool for db [{}] on host [{}]", dbName, creds.host());
        return newDs.getConnection();
    }
    public java.util.Map<String, List<Map<String, Object>>> executeBatchQueriesInParallel(java.util.Map<String, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto>> queryFilterMap) {
        if (queryFilterMap == null || queryFilterMap.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<String, List<Map<String, Object>>> results = new java.util.concurrent.ConcurrentHashMap<>();
        List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto>> entry : queryFilterMap.entrySet()) {
            String queryIdStr = entry.getKey();
            UUID queryId = ParsingUtils.parseUuid(queryIdStr);
            if (queryId != null) {
                futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        List<Map<String, Object>> data = self.executeQueryData(queryId, entry.getValue());
                        results.put(queryIdStr, data != null ? data : java.util.Collections.emptyList());
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).error("Async query execution failed for queryId {}: {}", queryIdStr, e.getMessage());
                        results.put(queryIdStr, java.util.Collections.emptyList());
                    }
                }));
            }
        }
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(QueryApplicationService.class).error("Batch query execution join error: {}", e.getMessage());
        }
        return results;
    }
    @Transactional(readOnly = true)
    public List<QueryResponseDto> getAllQueries() {
        return dataQueryRepository.findAll().stream()
                .map(queryMapper::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<QueryResponseDto> getAllByTenantIdQueries() {
        return dataQueryRepository.findAllByTenantId(TenantContext.get()).stream()
                .map(queryMapper::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<QueryResponseDto> getQueryById(UUID id) {
        return dataQueryRepository.findById(id).map(queryMapper::toDto);
    }
    @Transactional
    public QueryResponseDto createQuery(QueryRequestDto dto) {
        UserAccountEntity owner = currentUserService.getCurrentUser();
        DataQueryEntity query = new DataQueryEntity();
        query.setOwner(owner);
        applyDtoToQueryEntity(dto, query);
        DataQueryEntity savedQuery = dataQueryRepository.save(query);
        saveChildEntities(savedQuery, dto);
        auditApplicationService.logEvent("Création de requête", "QUERY", savedQuery.getId(), savedQuery.getQueryName(), null);
        return queryMapper.toDto(savedQuery);
    }
    @Transactional
    public Optional<QueryResponseDto> updateQuery(UUID id, QueryRequestDto dto) {
        return dataQueryRepository.findById(id).map(query -> {
            applyDtoToQueryEntity(dto, query);
            query.setUpdatedAt(java.time.Instant.now());
            DataQueryEntity updatedQuery = dataQueryRepository.save(query);
            deleteChildEntities(updatedQuery.getId());
            saveChildEntities(updatedQuery, dto);
            auditApplicationService.logEvent("Modification de requête", "QUERY", updatedQuery.getId(), updatedQuery.getQueryName(), null);
            FAST_QUERY_CACHE.clear();
            return queryMapper.toDto(updatedQuery);
        });
    }
    @Transactional
    public boolean deleteQuery(UUID id) {
        return dataQueryRepository.findById(id).map(query -> {
            List<com.dynamicdashboard.cockpit.dashboard.domain.WidgetEntity> widgets = widgetRepository.findByQueryId(id);
            for (com.dynamicdashboard.cockpit.dashboard.domain.WidgetEntity w : widgets) {
                w.setQuery(null);
                widgetRepository.save(w);
            }
            deleteChildEntities(query.getId());
            auditApplicationService.logEvent("Suppression de requête", "QUERY", query.getId(), query.getQueryName(), null);
            FAST_QUERY_CACHE.clear();
            dataQueryRepository.delete(query);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<QueryResponseDto> duplicateQuery(UUID id) {
        return dataQueryRepository.findById(id).map(source -> {
            QueryResponseDto sourceDto = queryMapper.toDto(source);
            QueryRequestDto copyDto = QueryRequestDto.builder()
                    .name(sourceDto.getName() + " (copie)")
                    .description(sourceDto.getDescription())
                    .visibility(sourceDto.getVisibility())
                    .sourceIds(sourceDto.getSourceIds() != null ? new ArrayList<>(sourceDto.getSourceIds()) : new ArrayList<>())
                    .joins(sourceDto.getJoins() != null ? new ArrayList<>(sourceDto.getJoins()) : new ArrayList<>())
                    .selectedFieldIds(sourceDto.getSelectedFieldIds() != null ? new ArrayList<>(sourceDto.getSelectedFieldIds()) : new ArrayList<>())
                    .conditions(sourceDto.getConditions() != null ? new ArrayList<>(sourceDto.getConditions()) : new ArrayList<>())
                    .transformations(sourceDto.getTransformations() != null ? new ArrayList<>(sourceDto.getTransformations()) : new ArrayList<>())
                    .groupByFieldIds(sourceDto.getGroupByFieldIds() != null ? new ArrayList<>(sourceDto.getGroupByFieldIds()) : new ArrayList<>())
                    .aggregation(sourceDto.getAggregation())
                    .aggregationFieldId(sourceDto.getAggregationFieldId())
                    .sort(sourceDto.getSort())
                    .rowLimit(sourceDto.getRowLimit())
                    .build();
            if (copyDto.getJoins() != null) {
                copyDto.getJoins().forEach(j -> j.setId(null));
            }
            if (copyDto.getConditions() != null) {
                copyDto.getConditions().forEach(c -> c.setId(null));
            }
            if (copyDto.getTransformations() != null) {
                copyDto.getTransformations().forEach(t -> t.setId(null));
            }
            QueryResponseDto created = createQuery(copyDto);
            auditApplicationService.logEvent("Duplication de requête", "QUERY", created.getId(), created.getName(), null);
            return created;
        });
    }
    private void applyDtoToQueryEntity(QueryRequestDto dto, DataQueryEntity query) {
        query.setQueryName(dto.getName() != null ? dto.getName() : "Nouvelle requête");
        query.setQueryDescription(dto.getDescription());
        query.setVisibility(ParsingUtils.parseEnum(QueryVisibility.class, dto.getVisibility(), QueryVisibility.SHARED));
        query.setAggregation(ParsingUtils.parseEnum(AggregationType.class, dto.getAggregation(), AggregationType.NONE));
        if (dto.getAggregationFieldId() != null && !dto.getAggregationFieldId().isBlank()) {
            resolveField(dto.getAggregationFieldId()).ifPresent(query::setAggregationField);
        } else {
            query.setAggregationField(null);
        }
        query.setRowLimit(dto.getRowLimit() > 0 ? dto.getRowLimit() : 100);
    }
    private void saveChildEntities(DataQueryEntity query, QueryRequestDto dto) {
        if (dto.getSourceIds() != null) {
            List<String> distinctSources = dto.getSourceIds().stream().distinct().toList();
            for (int i = 0; i < distinctSources.size(); i++) {
                String sourceIdStr = distinctSources.get(i);
                final int idx = i;
                resolveDataSource(sourceIdStr).ifPresent(ds -> {
                    QuerySourceBindingEntity sb = new QuerySourceBindingEntity();
                    sb.setQuery(query);
                    sb.setDataSource(ds);
                    sb.setPositionIndex(idx);
                    querySourceBindingRepository.save(sb);
                });
            }
        }
        if (dto.getSelectedFieldIds() != null) {
            for (int i = 0; i < dto.getSelectedFieldIds().size(); i++) {
                String fieldIdStr = dto.getSelectedFieldIds().get(i);
                final int idx = i;
                resolveField(fieldIdStr).ifPresent(df -> {
                    QuerySelectedFieldEntity sf = new QuerySelectedFieldEntity();
                    QuerySelectedFieldId sfId = new QuerySelectedFieldId();
                    sfId.setQueryId(query.getId());
                    sfId.setFieldId(df.getId());
                    sf.setId(sfId);
                    sf.setQuery(query);
                    sf.setField(df);
                    sf.setPositionIndex(idx);
                    querySelectedFieldRepository.save(sf);
                });
            }
        }
        if (dto.getGroupByFieldIds() != null) {
            for (int i = 0; i < dto.getGroupByFieldIds().size(); i++) {
                String fieldIdStr = dto.getGroupByFieldIds().get(i);
                final int idx = i;
                resolveField(fieldIdStr).ifPresent(df -> {
                    QueryGroupByFieldEntity gb = new QueryGroupByFieldEntity();
                    QueryGroupByFieldId gbId = new QueryGroupByFieldId();
                    gbId.setQueryId(query.getId());
                    gbId.setFieldId(df.getId());
                    gb.setId(gbId);
                    gb.setQuery(query);
                    gb.setField(df);
                    gb.setPositionIndex(idx);
                    queryGroupByFieldRepository.save(gb);
                });
            }
        }
        if (dto.getJoins() != null) {
            for (QueryJoinDto joinDto : dto.getJoins()) {
                QueryJoinEntity join = new QueryJoinEntity();
                join.setQuery(query);
                if (joinDto.getLeftSourceId() != null) resolveDataSource(joinDto.getLeftSourceId()).ifPresent(join::setLeftSource);
                if (joinDto.getLeftFieldId() != null) resolveField(joinDto.getLeftFieldId()).ifPresent(join::setLeftField);
                join.setJoinType(ParsingUtils.parseEnum(QueryJoinType.class, joinDto.getType(), QueryJoinType.INNER));
                if (joinDto.getRightSourceId() != null) resolveDataSource(joinDto.getRightSourceId()).ifPresent(join::setRightSource);
                if (joinDto.getRightFieldId() != null) resolveField(joinDto.getRightFieldId()).ifPresent(join::setRightField);
                queryJoinRepository.save(join);
            }
        }
        if (dto.getConditions() != null) {
            for (QueryConditionDto condDto : dto.getConditions()) {
                if (condDto.getFieldId() != null) {
                    resolveField(condDto.getFieldId()).ifPresent(field -> {
                        QueryConditionEntity cond = new QueryConditionEntity();
                        cond.setQuery(query);
                        cond.setField(field);
                        cond.setOperator(ParsingUtils.parseEnum(FilterOperator.class, condDto.getOperator(), FilterOperator.EQ));
                        cond.setValueExpression(condDto.getValue() != null ? condDto.getValue() : "");
                        cond.setLogicalOperator(ParsingUtils.parseEnum(LogicalOperator.class, condDto.getLogical(), LogicalOperator.AND));
                        cond.setParameterizable(condDto.isParametrable());
                        queryConditionRepository.save(cond);
                    });
                }
            }
        }
        if (dto.getTransformations() != null) {
            for (QueryTransformationDto trDto : dto.getTransformations()) {
                QueryTransformationEntity tr = new QueryTransformationEntity();
                tr.setQuery(query);
                tr.setTransformationType(ParsingUtils.parseEnum(QueryTransformationType.class, trDto.getType(), QueryTransformationType.RENAME));
                if (trDto.getFieldId() != null) resolveField(trDto.getFieldId()).ifPresent(tr::setTargetField);
                tr.setOutputLabel(trDto.getOutputLabel());
                tr.setFormulaExpression(trDto.getFormula());
                tr.setFormatOption(trDto.getFormat());
                tr.setReplacementValue(trDto.getReplacementValue());
                if (trDto.getOperator() != null) {
                    tr.setOperator(ParsingUtils.parseEnum(FilterOperator.class, trDto.getOperator(), null));
                }
                tr.setComparisonValue(trDto.getValue());
                queryTransformationRepository.save(tr);
            }
        }
        if (dto.getSort() != null && dto.getSort().getFieldId() != null) {
            String sortFieldId = dto.getSort().getFieldId();
            String resolveId = "__agg_value__".equals(sortFieldId)
                ? dto.getAggregationFieldId()
                : sortFieldId;
            if (resolveId != null) {
                resolveField(resolveId).ifPresent(field -> {
                    QuerySortEntity sort = querySortRepository.findById(query.getId()).orElseGet(() -> {
                        QuerySortEntity s = new QuerySortEntity();
                        s.setQuery(query);
                        return s;
                    });
                    sort.setField(field);
                    sort.setDirection(ParsingUtils.parseEnum(SortDirection.class, dto.getSort().getDirection(), SortDirection.ASC));
                    querySortRepository.save(sort);
                });
            }
        }
    }
    private Optional<DataFieldEntity> resolveField(String fieldIdStr) {
        if (fieldIdStr == null || fieldIdStr.isBlank()) return Optional.empty();
        UUID uuid = ParsingUtils.parseUuid(fieldIdStr);
        if (uuid != null) {
            Optional<DataFieldEntity> byId = dataFieldRepository.findById(uuid);
            if (byId.isPresent()) return byId;
        }
        Optional<DataFieldEntity> byKey = dataFieldRepository.findFirstByFieldKey(fieldIdStr);
        if (byKey.isPresent()) return byKey;
        if (fieldIdStr.contains(".")) {
            String shortKey = fieldIdStr.substring(fieldIdStr.lastIndexOf('.') + 1);
            Optional<DataFieldEntity> byShortKey = dataFieldRepository.findFirstByFieldKey(shortKey);
            if (byShortKey.isPresent()) return byShortKey;
        }
        return dataFieldRepository.findFirstByFieldLabel(fieldIdStr);
    }
    private Optional<com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity> resolveDataSource(String sourceIdStr) {
        if (sourceIdStr == null || sourceIdStr.isBlank()) return Optional.empty();
        UUID uuid = ParsingUtils.parseUuid(sourceIdStr);
        if (uuid != null) {
            Optional<com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity> byId = dataSourceRepository.findById(uuid);
            if (byId.isPresent()) return byId;
        }
        Optional<com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity> byKey = dataSourceRepository.findBySourceKey(sourceIdStr);
        if (byKey.isPresent()) return byKey;
        return dataSourceRepository.findFirstBySourceLabel(sourceIdStr);
    }
    private void deleteChildEntities(UUID queryId) {
        querySourceBindingRepository.deleteAll(querySourceBindingRepository.findByQueryIdOrderByPositionIndexAsc(queryId));
        querySourceBindingRepository.flush();
        querySelectedFieldRepository.deleteAll(querySelectedFieldRepository.findByIdQueryIdOrderByPositionIndexAsc(queryId));
        querySelectedFieldRepository.flush();
        queryGroupByFieldRepository.deleteAll(queryGroupByFieldRepository.findByIdQueryIdOrderByPositionIndexAsc(queryId));
        queryGroupByFieldRepository.flush();
        queryJoinRepository.deleteAll(queryJoinRepository.findByQueryId(queryId));
        queryJoinRepository.flush();
        queryConditionRepository.deleteAll(queryConditionRepository.findByQueryId(queryId));
        queryConditionRepository.flush();
        queryTransformationRepository.deleteAll(queryTransformationRepository.findByQueryId(queryId));
        queryTransformationRepository.flush();
        querySortRepository.findById(queryId).ifPresent(s -> {
            querySortRepository.delete(s);
            querySortRepository.flush();
        });
    }
    private String adjustSqlForDialect(String sql, com.dynamicdashboard.cockpit.datasource.domain.DbType dbType, Integer limit) {
        if (sql == null) return "";
        if (dbType == com.dynamicdashboard.cockpit.datasource.domain.DbType.SQL_SERVER) {
            String result = sql.replaceAll("\"([^\"]+)\"", "[$1]");
            result = result.replaceAll("(?i)extract\\s*\\(\\s*year\\s+from\\s+([^\\)]+)\\)", "YEAR($1)");
            result = result.replaceAll("(?i)extract\\s*\\(\\s*month\\s+from\\s+([^\\)]+)\\)", "MONTH($1)");
            result = result.replaceAll("(?i)extract\\s*\\(\\s*quarter\\s+from\\s+([^\\)]+)\\)", "DATEPART(quarter, $1)");
            result = result.replaceAll("(?i)\\s+limit\\s+\\d+", "");
            result = result.replaceAll("(?i)\\s+fetch\\s+next\\s+\\d+\\s+rows\\s+only", "");
            if (limit != null && limit > 0) {
                if (result.toLowerCase().startsWith("select distinct ")) {
                    result = "select distinct TOP (" + limit + ") " + result.substring(16);
                } else if (result.toLowerCase().startsWith("select ")) {
                    result = "select TOP (" + limit + ") " + result.substring(7);
                }
            }
            return result;
        }
        return sql;
    }
    private String getPhysicalTableName(com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity ds) {
        if (ds == null) return "";
        if (ds.getSourceLabel() != null && !ds.getSourceLabel().isBlank()) {
            return ds.getSourceLabel();
        }
        if (ds.getSourceKey() != null && ds.getDbConnection() != null && ds.getDbConnection().getId() != null) {
            String prefix = ds.getDbConnection().getId().toString() + "_";
            if (ds.getSourceKey().startsWith(prefix)) {
                return ds.getSourceKey().substring(prefix.length());
            }
        }
        return ds.getSourceKey() != null ? ds.getSourceKey() : "";
    }
    private String getFieldColumnName(DataFieldEntity field) {
        if (field == null) return "";
        String table = getPhysicalTableName(field.getDataSource());
        if (!table.isBlank()) {
            return table + "." + field.getFieldKey();
        }
        return field.getFieldKey();
    }
    private org.jooq.Field<?> applyDateTransformation(org.jooq.Field<?> field, DataFieldEntity df, List<QueryTransformationDto> transformations) {
        if (transformations == null || transformations.isEmpty() || df == null) return field;
        String idStr = df.getId() != null ? df.getId().toString() : "";
        String keyStr = df.getFieldKey() != null ? df.getFieldKey() : "";
        String tableKeyStr = (df.getDataSource() != null ? getPhysicalTableName(df.getDataSource()) + "." : "") + keyStr;
        for (QueryTransformationDto tr : transformations) {
            if (tr.getFieldId() != null && "format".equalsIgnoreCase(tr.getType()) && tr.getFormat() != null) {
                String trFid = tr.getFieldId();
                boolean match = trFid.equalsIgnoreCase(idStr) ||
                                trFid.equalsIgnoreCase(keyStr) ||
                                trFid.equalsIgnoreCase(tableKeyStr) ||
                                trFid.endsWith("." + keyStr);
                if (match) {
                    switch (tr.getFormat().toLowerCase()) {
                        case "year": return org.jooq.impl.DSL.year(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        case "month": return org.jooq.impl.DSL.month(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        case "quarter": return org.jooq.impl.DSL.quarter(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        default: break;
                    }
                }
            }
        }
        return field;
    }
    private org.jooq.Field<?> applyDateTransformationEntity(org.jooq.Field<?> field, DataFieldEntity df, List<QueryTransformationEntity> transformations) {
        if (transformations == null || transformations.isEmpty() || df == null) return field;
        String idStr = df.getId() != null ? df.getId().toString() : "";
        String keyStr = df.getFieldKey() != null ? df.getFieldKey() : "";
        String tableKeyStr = (df.getDataSource() != null ? getPhysicalTableName(df.getDataSource()) + "." : "") + keyStr;
        for (QueryTransformationEntity tr : transformations) {
            if (tr.getTargetField() != null) {
                String targetId = tr.getTargetField().getId() != null ? tr.getTargetField().getId().toString() : "";
                String targetKey = tr.getTargetField().getFieldKey() != null ? tr.getTargetField().getFieldKey() : "";
                boolean match = idStr.equalsIgnoreCase(targetId) ||
                                keyStr.equalsIgnoreCase(targetKey) ||
                                tableKeyStr.equalsIgnoreCase(targetKey) ||
                                targetKey.endsWith("." + keyStr);
                if (match && com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryTransformationType.FORMAT.equals(tr.getTransformationType()) && tr.getFormatOption() != null) {
                    switch (tr.getFormatOption().toLowerCase()) {
                        case "year": return org.jooq.impl.DSL.year(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        case "month": return org.jooq.impl.DSL.month(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        case "quarter": return org.jooq.impl.DSL.quarter(org.jooq.impl.DSL.field(field.getName(), java.sql.Date.class)).cast(String.class);
                        default: break;
                    }
                }
            }
        }
        return field;
    }
    private org.jooq.Table<org.jooq.Record> buildJooqTable(String tableName) {
        if (tableName == null || tableName.isBlank()) return org.jooq.impl.DSL.table(org.jooq.impl.DSL.name(""));
        return org.jooq.impl.DSL.table(org.jooq.impl.DSL.name(tableName));
    }
    private org.jooq.Field<Object> buildJooqField(String colExpr) {
        if (colExpr == null || colExpr.isBlank() || "*".equals(colExpr)) {
            return org.jooq.impl.DSL.field("*");
        }
        if (colExpr.contains(".")) {
            String[] parts = colExpr.split("\\.", 2);
            return org.jooq.impl.DSL.field(org.jooq.impl.DSL.name(parts[0], parts[1]));
        }
        return org.jooq.impl.DSL.field(org.jooq.impl.DSL.name(colExpr));
    }
    private <T> org.jooq.Field<T> buildJooqField(String colExpr, Class<T> type) {
        if (colExpr == null || colExpr.isBlank() || "*".equals(colExpr)) {
            return org.jooq.impl.DSL.field("*", type);
        }
        if (colExpr.contains(".")) {
            String[] parts = colExpr.split("\\.", 2);
            return org.jooq.impl.DSL.field(org.jooq.impl.DSL.name(parts[0], parts[1]), type);
        }
        return org.jooq.impl.DSL.field(org.jooq.impl.DSL.name(colExpr), type);
    }
}
