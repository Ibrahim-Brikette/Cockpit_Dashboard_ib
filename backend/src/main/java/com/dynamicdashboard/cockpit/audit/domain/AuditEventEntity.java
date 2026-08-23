package com.dynamicdashboard.cockpit.audit.domain;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_event", schema = "cockpit")
public class AuditEventEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccountEntity actorUser;
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;
    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;
    @Column(name = "target_id")
    private UUID targetId;
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;
    @Column(name = "source_ip", length = 64)
    private String sourceIp;
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
