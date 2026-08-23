package com.dynamicdashboard.cockpit.dashboard.domain;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DataGridDensity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "widget_datagrid_config", schema = "cockpit")
public class WidgetDatagridConfigEntity {
    @Id
    @Column(name = "widget_id", nullable = false)
    private UUID widgetId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "widget_id", nullable = false)
    private WidgetEntity widget;
    @Column(name = "rows_per_page")
    private Integer rowsPerPage;
    @Enumerated(EnumType.STRING)
    @Column(name = "density", length = 24)
    private DataGridDensity density;
    @Column(name = "show_toolbar")
    private Boolean showToolbar;
    @Column(name = "show_search")
    private Boolean showSearch;
    @Column(name = "show_pagination")
    private Boolean showPagination;
    @Column(name = "show_totals")
    private Boolean showTotals;
    @Column(name = "sortable")
    private Boolean sortable;
    @Column(name = "filterable")
    private Boolean filterable;
}
