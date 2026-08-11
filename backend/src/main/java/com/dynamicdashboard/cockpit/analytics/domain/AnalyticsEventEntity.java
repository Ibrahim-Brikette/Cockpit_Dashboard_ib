package com.dynamicdashboard.cockpit.analytics.domain;

import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AnalyticsAction;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AnalyticsTarget;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analytics_event", schema = "cockpit")
public class AnalyticsEventEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccountEntity actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private AnalyticsAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 24)
    private AnalyticsTarget targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id")
    private DashboardEntity dashboard;

    @Column(name = "dashboard_name", length = 180)
    private String dashboardName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}