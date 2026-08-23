package com.dynamicdashboard.cockpit.datasource.application;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity;
import com.dynamicdashboard.cockpit.catalog.repository.DataFieldRepository;
import com.dynamicdashboard.cockpit.catalog.repository.DataSourceRepository;
import com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity;
import com.dynamicdashboard.cockpit.datasource.domain.DbType;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaExtractor {
    private final DataSourceRepository dataSourceRepository;
    private final DataFieldRepository dataFieldRepository;
    @Getter
    @AllArgsConstructor
    private static class ColumnInfo {
        private String columnName;
        private String dataType;
        private boolean nullable;
    }
    @Getter
    @AllArgsConstructor
    private static class TableSchemaInfo {
        private String tableName;
        private List<ColumnInfo> columns;
    }
    @Transactional(readOnly = true)
    public List<Map<String, String>> previewSchema(DbConnectionEntity connectionEntity, String rawPassword) throws Exception {
        List<Map<String, String>> schemaList = new ArrayList<>();
        String jdbcUrl = buildJdbcUrl(connectionEntity);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionEntity.getDbUsername(), rawPassword)) {
            List<TableSchemaInfo> tables = extractTablesFromDb(conn, connectionEntity);
            for (TableSchemaInfo t : tables) {
                for (ColumnInfo c : t.getColumns()) {
                    if (schemaList.size() >= 100) break;
                    schemaList.add(Map.of(
                        "tableName", t.getTableName(),
                        "fieldName", c.getColumnName(),
                        "fieldType", mapSqlTypeToFieldType(c.getDataType()).name().toLowerCase()
                    ));
                }
            }
        }
        return schemaList;
    }
    @Transactional(readOnly = true)
    public boolean testConnection(DbConnectionEntity connectionEntity, String rawPassword) throws Exception {
        String jdbcUrl = buildJdbcUrl(connectionEntity);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionEntity.getDbUsername(), rawPassword)) {
            return conn.isValid(5);
        }
    }
    @Transactional
    public void extractAndSaveSchema(DbConnectionEntity connectionEntity, String rawPassword) {
        String jdbcUrl = buildJdbcUrl(connectionEntity);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connectionEntity.getDbUsername(), rawPassword)) {
            List<TableSchemaInfo> tablesList = extractTablesFromDb(conn, connectionEntity);
            log.info("Extracting schema for {}: found {} tables.", connectionEntity.getConnectionName(), tablesList.size());
            for (TableSchemaInfo tableInfo : tablesList) {
                String tableName = tableInfo.getTableName();
                String uniqueSourceKey = (connectionEntity.getId() != null)
                    ? connectionEntity.getId().toString() + "_" + tableName 
                    : connectionEntity.getConnectionName().replaceAll("\\s+", "_").toLowerCase() + "_" + tableName;
                DataSourceEntity dataSource = dataSourceRepository.findBySourceKey(uniqueSourceKey)
                    .orElseGet(() -> {
                        DataSourceEntity newDs = new DataSourceEntity();
                        newDs.setCreatedBy("system");
                        return newDs;
                    });
                dataSource.setUpdatedBy("system");
                dataSource.setSourceKey(uniqueSourceKey);
                dataSource.setSourceLabel(tableName);
                dataSource.setSourceDescription(tableName);
                String hostApp = connectionEntity.getDbName();
                if (hostApp != null && hostApp.length() > 40) {
                    hostApp = hostApp.substring(0, 40);
                }
                dataSource.setHostApplication(hostApp != null && !hostApp.isBlank() ? hostApp : "Database");
                dataSource.setActive(true);
                dataSource.setDbConnection(connectionEntity);
                dataSource = dataSourceRepository.save(dataSource);
                Map<String, DataFieldEntity> existingFieldMap = dataFieldRepository.findByDataSourceId(dataSource.getId())
                        .stream().collect(Collectors.toMap(DataFieldEntity::getFieldKey, f -> f, (a, b) -> a));
                for (ColumnInfo col : tableInfo.getColumns()) {
                    DataFieldEntity field = existingFieldMap.getOrDefault(col.getColumnName(), new DataFieldEntity());
                    if (field.getId() == null) {
                        field.setCreatedBy("system");
                    }
                    field.setUpdatedBy("system");
                    field.setDataSource(dataSource);
                    field.setFieldKey(col.getColumnName());
                    field.setFieldLabel(col.getColumnName());
                    field.setFieldDescription(col.getColumnName());
                    field.setFieldType(mapSqlTypeToFieldType(col.getDataType()));
                    field.setNullable(col.isNullable());
                    dataFieldRepository.save(field);
                }
            }
            log.info("Successfully saved {} extracted tables into catalog for connection {}.", tablesList.size(), connectionEntity.getConnectionName());
        } catch (Exception e) {
            log.error("Failed to extract schema for connection {}", connectionEntity.getConnectionName(), e);
            throw new RuntimeException("Schema extraction failed: " + e.getMessage());
        }
    }
    private List<TableSchemaInfo> extractTablesFromDb(Connection conn, DbConnectionEntity connectionEntity) {
        List<TableSchemaInfo> result = new ArrayList<>();
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = (connectionEntity.getDbType() == DbType.MYSQL) ? connectionEntity.getDbName() : null;
            String[] types = new String[]{"TABLE", "PARTITIONED TABLE", "VIEW", "FOREIGN TABLE", "MATERIALIZED VIEW"};
            try (ResultSet tables = metaData.getTables(catalog, null, "%", types)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableType = null;
                    try { tableType = tables.getString("TABLE_TYPE"); } catch (Exception ignored) {}
                    String tableSchem = null;
                    try { tableSchem = tables.getString("TABLE_SCHEM"); } catch (Exception ignored) {}
                    if (tableType != null && tableType.toUpperCase().contains("SYSTEM")) {
                        continue;
                    }
                    if (tableSchem != null && (tableSchem.equalsIgnoreCase("information_schema") 
                            || tableSchem.equalsIgnoreCase("pg_catalog") 
                            || tableSchem.equalsIgnoreCase("pg_toast")
                            || tableSchem.equalsIgnoreCase("sys"))) {
                        continue;
                    }
                    if (tableName.startsWith("pg_") || tableName.startsWith("sql_") || tableName.startsWith("information_schema") || tableName.startsWith("sys")) {
                        continue;
                    }
                    List<ColumnInfo> columns = new ArrayList<>();
                    try (ResultSet cols = metaData.getColumns(catalog, tableSchem, tableName, "%")) {
                        while (cols.next()) {
                            String colName = cols.getString("COLUMN_NAME");
                            String dataTypeName = cols.getString("TYPE_NAME");
                            int nullable = cols.getInt("NULLABLE");
                            columns.add(new ColumnInfo(colName, dataTypeName, nullable == DatabaseMetaData.columnNullable));
                        }
                    }
                    result.add(new TableSchemaInfo(tableName, columns));
                }
            }
        } catch (Exception e) {
            log.warn("JDBC metadata query failed for {}, trying information_schema fallback: {}", connectionEntity.getConnectionName(), e.getMessage());
        }
        if (result.isEmpty()) {
            String sqlTables = "SELECT table_name, table_schema FROM information_schema.tables " +
                               "WHERE LOWER(table_schema) NOT IN ('pg_catalog', 'information_schema', 'sys', 'pg_toast', 'pg_temp_1') " +
                               "AND LOWER(table_name) NOT LIKE 'pg_%' AND LOWER(table_name) NOT LIKE 'sql_%'";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTables);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tName = rs.getString("table_name");
                    String tSchema = rs.getString("table_schema");
                    List<ColumnInfo> cols = new ArrayList<>();
                    String sqlCols = "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
                    try (PreparedStatement colStmt = conn.prepareStatement(sqlCols)) {
                        colStmt.setString(1, tSchema);
                        colStmt.setString(2, tName);
                        try (ResultSet colRs = colStmt.executeQuery()) {
                            while (colRs.next()) {
                                String cName = colRs.getString("column_name");
                                String cType = colRs.getString("data_type");
                                String isNull = colRs.getString("is_nullable");
                                cols.add(new ColumnInfo(cName, cType, "YES".equalsIgnoreCase(isNull)));
                            }
                        }
                    }
                    result.add(new TableSchemaInfo(tName, cols));
                }
            } catch (Exception e) {
                log.error("information_schema fallback query failed for {}: {}", connectionEntity.getConnectionName(), e.getMessage());
            }
        }
        return result;
    }
    private String buildJdbcUrl(DbConnectionEntity entity) {
        switch (entity.getDbType()) {
            case POSTGRESQL:
                String pgSsl = entity.isUseSsl() ? "?ssl=true&sslmode=require" : "?ssl=false";
                return String.format("jdbc:postgresql://%s:%d/%s%s",
                        entity.getDbHost(), entity.getDbPort(), entity.getDbName(), pgSsl);
            case MYSQL:
                String mysqlSsl = entity.isUseSsl() ? "?useSSL=true" : "?useSSL=false&allowPublicKeyRetrieval=true";
                return String.format("jdbc:mysql://%s:%d/%s%s",
                        entity.getDbHost(), entity.getDbPort(), entity.getDbName(), mysqlSsl);
            case SQL_SERVER:
                String sqlSsl = entity.isUseSsl() ? ";encrypt=true;trustServerCertificate=true" : ";encrypt=false;trustServerCertificate=true";
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s%s",
                        entity.getDbHost(), entity.getDbPort(), entity.getDbName(), sqlSsl);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + entity.getDbType());
        }
    }
    private FieldType mapSqlTypeToFieldType(String typeName) {
        if (typeName == null) return FieldType.TEXT;
        String lower = typeName.toLowerCase();
        if (lower.contains("int") || lower.contains("double") || lower.contains("float") || lower.contains("decimal") || lower.contains("numeric") || lower.contains("real")) {
            return FieldType.NUMBER;
        }
        if (lower.contains("date") || lower.contains("time") || lower.contains("timestamp")) {
            return FieldType.DATE;
        }
        return FieldType.TEXT;
    }
}
