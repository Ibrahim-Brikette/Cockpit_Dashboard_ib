package com.dynamicdashboard.cockpit.alert.domain;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertComparisonOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertMetric;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertSeverity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertStatus;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "alert_event", schema = "cockpit")
public class AlertEventEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AlertRuleEntity rule;

    @Column(name = "rule_name", nullable = false, length = 160)
    private String ruleName;

    @Column(name = "query_name", nullable = false, length = 160)
    private String queryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 24)
    private AlertMetric metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 8)
    private AlertComparisonOperator operator;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Column(name = "observed_value", nullable = false)
    private double observedValue;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;
}