package com.dynamicdashboard.cockpit.dashboard.domain;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DashboardDensity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DashboardStatus;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.RefreshInterval;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dashboard", schema = "cockpit")

@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DashboardEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccountEntity owner;
    @Column(name = "dashboard_name", nullable = false, length = 180)
    private String dashboardName;
    @Column(name = "dashboard_description", length = 500)
    private String dashboardDescription;
    @Column(name = "color_hex", nullable = false, length = 12)
    private String colorHex;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private DashboardStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "share_level", nullable = false, length = 24)
    private ShareLevel shareLevel;
    @Column(name = "columns_count", nullable = false)
    private int columnsCount;
    @Enumerated(EnumType.STRING)
    @Column(name = "density", nullable = false, length = 24)
    private DashboardDensity density;
    @Enumerated(EnumType.STRING)
    @Column(name = "refresh_interval", nullable = false, length = 24)
    private RefreshInterval refreshInterval;
    @Column(name = "favorite", nullable = false)
    private boolean favorite;
    @Column(name = "archived", nullable = false)
    private boolean archived;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
