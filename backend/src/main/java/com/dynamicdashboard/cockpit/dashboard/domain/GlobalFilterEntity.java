package com.dynamicdashboard.cockpit.dashboard.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.GlobalFilterInput;
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
@Table(name = "global_filter", schema = "cockpit")
public class GlobalFilterEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardEntity dashboard;
    @Column(name = "filter_name", nullable = false, length = 120)
    private String filterName;
    @Column(name = "filter_label", nullable = false, length = 160)
    private String filterLabel;
    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 24)
    private GlobalFilterInput inputType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_field_id")
    private DataFieldEntity targetField;
    @Column(name = "default_value", nullable = false, length = 255)
    private String defaultValue;
    @Column(name = "reader_visible", nullable = false)
    private boolean readerVisible;
}
