package com.dynamicdashboard.cockpit.datasource.application.dto;
import com.dynamicdashboard.cockpit.datasource.domain.DbType;
import lombok.Data;
import java.util.UUID;
@Data
public class DbConnectionResponseDto {
    private UUID id;
    private String connectionName;
    private DbType dbType;
    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private boolean useSsl;
    private long tableCount;
}
