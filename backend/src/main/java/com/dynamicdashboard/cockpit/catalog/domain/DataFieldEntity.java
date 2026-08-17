package com.dynamicdashboard.cockpit.catalog.domain;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FieldType;
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
@Table(name = "data_field", schema = "cockpit")
public class DataFieldEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;
    @Column(name = "field_key", nullable = false, length = 120)
    private String fieldKey;
    @Column(name = "field_label", nullable = false, length = 160)
    private String fieldLabel;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 24)
    private FieldType fieldType;
    @Column(name = "field_description", length = 255)
    private String fieldDescription;
    @Column(name = "nullable", nullable = false)
    private boolean nullable;
}
