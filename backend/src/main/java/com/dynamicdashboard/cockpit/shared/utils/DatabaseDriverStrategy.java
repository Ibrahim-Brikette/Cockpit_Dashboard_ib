package com.dynamicdashboard.cockpit.shared.utils;
import com.dynamicdashboard.cockpit.datasource.domain.DbType;
import org.jooq.SQLDialect;
import java.util.List;
public enum DatabaseDriverStrategy {
    POSTGRESQL(
        "org.postgresql.Driver",
        SQLDialect.POSTGRES,
        List.of("cockpit_pg", "postgres"),
        (host, port, db) -> String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=3", host, port, db)
    ),
    MYSQL(
        "com.mysql.cj.jdbc.Driver",
        SQLDialect.MYSQL,
        List.of("cockpit_mysql"),
        (host, port, db) -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=3000", host, port, db)
    ),
    SQL_SERVER(
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        SQLDialect.DEFAULT,
        List.of("cockpit_sqlserver"),
        (host, port, db) -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;loginTimeout=3", host, port, db)
    );
    private final String driverClassName;
    private final SQLDialect dialect;
    private final List<String> containerCandidates;
    private final UrlBuilder urlBuilder;
    @FunctionalInterface
    public interface UrlBuilder {
        String build(String host, int port, String dbName);
    }
    DatabaseDriverStrategy(String driverClassName, SQLDialect dialect, List<String> containerCandidates, UrlBuilder urlBuilder) {
        this.driverClassName = driverClassName;
        this.dialect = dialect;
        this.containerCandidates = containerCandidates;
        this.urlBuilder = urlBuilder;
    }
    public String getDriverClassName() {
        return driverClassName;
    }
    public SQLDialect getDialect() {
        return dialect;
    }
    public List<String> getContainerCandidates() {
        return containerCandidates;
    }
    public String buildUrl(String host, int port, String dbName) {
        return urlBuilder.build(host, port, dbName);
    }
    public static DatabaseDriverStrategy from(DbType dbType) {
        if (dbType == null) return POSTGRESQL;
        return switch (dbType) {
            case MYSQL -> MYSQL;
            case SQL_SERVER -> SQL_SERVER;
            default -> POSTGRESQL;
        };
    }
    public static DatabaseDriverStrategy from(String typeName) {
        if (typeName == null) return POSTGRESQL;
        if ("MYSQL".equalsIgnoreCase(typeName)) return MYSQL;
        if ("SQL_SERVER".equalsIgnoreCase(typeName)) return SQL_SERVER;
        return POSTGRESQL;
    }
}
