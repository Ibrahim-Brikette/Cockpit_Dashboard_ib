package com.dynamicdashboard.cockpit.alert.domain;

import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertComparisonOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertMetric;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertSeverity;
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

@Getter
@Setter
@Entity
@Table(name = "alert_rule", schema = "cockpit")
public class AlertRuleEntity extends AuditableEntity {

    @Column(name = "rule_name", nullable = false, length = 160)
    private String ruleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private DataQueryEntity query;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 24)
    private AlertMetric metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 8)
    private AlertComparisonOperator operator;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private AlertSeverity severity;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}