package com.dynamicdashboard.cockpit.datasource.application.dto;
import com.dynamicdashboard.cockpit.datasource.domain.DbType;
import lombok.Data;
@Data
public class DataSourceConnectionRequestDto {
    private String connectionName;
    private DbType dbType;
    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private boolean useSsl;
}
