package com.dynamicdashboard.cockpit.query.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
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
@Table(name = "query_group_by_field", schema = "cockpit")
public class QueryGroupByFieldEntity {
    @EmbeddedId
    private QueryGroupByFieldId id = new QueryGroupByFieldId();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("queryId")
    @JoinColumn(name = "query_id", nullable = false)
    private DataQueryEntity query;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("fieldId")
    @JoinColumn(name = "field_id", nullable = false)
    private DataFieldEntity field;
    @Column(name = "position_index", nullable = false)
    private int positionIndex;
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class QueryGroupByFieldId implements Serializable {
        @Column(name = "query_id", nullable = false)
        private UUID queryId;
        @Column(name = "field_id", nullable = false)
        private UUID fieldId;
    }
}
