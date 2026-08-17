package com.dynamicdashboard.cockpit.dashboard.domain;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.KpiFormat;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.RefreshInterval;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.WidgetType;
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

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "widget", schema = "cockpit")
@Filter(name =  "tenantFilter", condition = "tenant_id = :tenantId")
public class WidgetEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardEntity dashboard;
    @Enumerated(EnumType.STRING)
    @Column(name = "widget_type", nullable = false, length = 24)
    private WidgetType widgetType;
    @Column(name = "widget_title", nullable = false, length = 180)
    private String widgetTitle;
    @Column(name = "show_title", nullable = false)
    private boolean showTitle;
    @Column(name = "widget_description", length = 500)
    private String widgetDescription;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private DataQueryEntity query;
    @Column(name = "grid_x", nullable = false)
    private int gridX;
    @Column(name = "grid_y", nullable = false)
    private int gridY;
    @Column(name = "grid_w", nullable = false)
    private int gridW;
    @Column(name = "grid_h", nullable = false)
    private int gridH;
    @Enumerated(EnumType.STRING)
    @Column(name = "refresh_interval", nullable = false, length = 24)
    private RefreshInterval refreshInterval;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navigate_to_dashboard_id")
    private DashboardEntity navigateToDashboard;
    @Enumerated(EnumType.STRING)
    @Column(name = "kpi_format", length = 24)
    private KpiFormat kpiFormat;
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;
    @Column(name = "tenant_id",nullable = false)
    private UUID tenantId;
}
