package com.dynamicdashboard.cockpit.alert.domain;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;
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
@Table(name = "alert_rule_channel", schema = "cockpit")
public class AlertRuleChannelEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRuleEntity rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private AlertChannel channel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "recipient", length = 255)
    private String recipient;
}