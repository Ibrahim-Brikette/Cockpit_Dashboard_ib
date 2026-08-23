package com.dynamicdashboard.cockpit.dashboard.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "dashboard_tag", schema = "cockpit")
public class DashboardTagEntity {
    @EmbeddedId
    private DashboardTagId id = new DashboardTagId();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("dashboardId")
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardEntity dashboard;
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class DashboardTagId implements Serializable {
        @Column(name = "dashboard_id", nullable = false)
        private UUID dashboardId;
        @Column(name = "tag_value", nullable = false, length = 80)
        private String tagValue;
    }
}
