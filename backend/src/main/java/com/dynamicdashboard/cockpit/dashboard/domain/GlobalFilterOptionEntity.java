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
@Table(name = "global_filter_option", schema = "cockpit")
public class GlobalFilterOptionEntity {
    @EmbeddedId
    private GlobalFilterOptionId id = new GlobalFilterOptionId();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("filterId")
    @JoinColumn(name = "filter_id", nullable = false)
    private GlobalFilterEntity filter;
    @Column(name = "position_index", nullable = false)
    private int positionIndex;
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class GlobalFilterOptionId implements Serializable {
        @Column(name = "filter_id", nullable = false)
        private UUID filterId;
        @Column(name = "option_value", nullable = false, length = 160)
        private String optionValue;
    }
}
