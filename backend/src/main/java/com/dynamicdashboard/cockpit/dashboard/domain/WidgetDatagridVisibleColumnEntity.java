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
@Table(name = "widget_datagrid_visible_column", schema = "cockpit")
public class WidgetDatagridVisibleColumnEntity {
    @EmbeddedId
    private WidgetDatagridVisibleColumnId id = new WidgetDatagridVisibleColumnId();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("widgetId")
    @JoinColumn(name = "widget_id", nullable = false)
    private WidgetDatagridConfigEntity widgetDatagridConfig;
    @Column(name = "position_index", nullable = false)
    private int positionIndex;
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class WidgetDatagridVisibleColumnId implements Serializable {
        @Column(name = "widget_id", nullable = false)
        private UUID widgetId;
        @Column(name = "column_name", nullable = false, length = 160)
        private String columnName;
    }
}
