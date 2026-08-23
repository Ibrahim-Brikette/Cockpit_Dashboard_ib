package com.dynamicdashboard.cockpit.datasource.domain;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "db_connection", schema = "cockpit")
@Getter
@Setter
public class DbConnectionEntity extends AuditableEntity {
    @Column(name = "connection_name", nullable = false)
    private String connectionName;
    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false)
    private DbType dbType;
    @Column(name = "db_host", nullable = false)
    private String dbHost;
    @Column(name = "db_port", nullable = false)
    private Integer dbPort;
    @Column(name = "db_name", nullable = false)
    private String dbName;
    @Column(name = "db_username", nullable = false)
    private String dbUsername;
    @Column(name = "vault_secret_key", nullable = false)
    private String vaultSecretKey;
    @Column(name = "use_ssl", nullable = false)
    private boolean useSsl;
}
