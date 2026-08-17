package com.dynamicdashboard.cockpit.catalog.domain;
import com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "data_source", schema = "cockpit")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DataSourceEntity extends AuditableEntity {
    @Column(name = "source_key", nullable = false, length = 120, unique = true)
    private String sourceKey;
    @Column(name = "source_label", nullable = false, length = 160)
    private String sourceLabel;
    @Column(name = "source_description", length = 400)
    private String sourceDescription;
    @Column(name = "host_application", nullable = false, length = 40)
    private String hostApplication;
    @Column(name = "active", nullable = false)
    private boolean active;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "db_connection_id")
    private DbConnectionEntity dbConnection;
    @Column(name = "tenant_id",nullable = false)
    private UUID tenantId;
}
