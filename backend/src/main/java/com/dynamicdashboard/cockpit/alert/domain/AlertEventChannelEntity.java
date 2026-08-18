package com.dynamicdashboard.cockpit.alert.domain;

import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "alert_event_channel", schema = "cockpit")
public class AlertEventChannelEntity {

    @EmbeddedId
    private AlertEventChannelId id = new AlertEventChannelId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private AlertEventEntity event;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class AlertEventChannelId implements Serializable {

        @Column(name = "event_id", nullable = false)
        private UUID eventId;

        @Enumerated(EnumType.STRING)
        @Column(name = "channel", nullable = false, length = 16)
        private AlertChannel channel;
    }
}