package com.dynamicdashboard.cockpit.dashboard.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FilterOperator;
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
@Table(name = "widget_filter", schema = "cockpit")
public class WidgetFilterEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "widget_id", nullable = false)
    private WidgetEntity widget;
    @Column(name = "filter_label", length = 160)
    private String filterLabel;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_field_id")
    private DataFieldEntity targetField;
    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 24)
    private FilterOperator operator;
    @Column(name = "filter_value", nullable = false, length = 500)
    private String filterValue;
    @Column(name = "position_index", nullable = false)
    private int positionIndex;
}
