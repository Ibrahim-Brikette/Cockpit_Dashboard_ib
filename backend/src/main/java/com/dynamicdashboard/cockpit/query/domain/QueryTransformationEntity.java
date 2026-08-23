package com.dynamicdashboard.cockpit.query.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FilterOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryTransformationType;
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
@Table(name = "query_transformation", schema = "cockpit")
public class QueryTransformationEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private DataQueryEntity query;
    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 24)
    private QueryTransformationType transformationType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_field_id")
    private DataFieldEntity targetField;
    @Column(name = "output_label", length = 160)
    private String outputLabel;
    @Column(name = "formula_expression", length = 800)
    private String formulaExpression;
    @Column(name = "format_option", length = 24)
    private String formatOption;
    @Column(name = "replacement_value", length = 255)
    private String replacementValue;
    @Enumerated(EnumType.STRING)
    @Column(name = "operator", length = 24)
    private FilterOperator operator;
    @Column(name = "comparison_value", length = 255)
    private String comparisonValue;
}
