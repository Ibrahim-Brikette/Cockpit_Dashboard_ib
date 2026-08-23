package com.dynamicdashboard.cockpit.query.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FilterOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.LogicalOperator;
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
@Table(name = "query_condition", schema = "cockpit")
public class QueryConditionEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private DataQueryEntity query;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private DataFieldEntity field;
    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 24)
    private FilterOperator operator;
    @Column(name = "value_expression", nullable = false, length = 500)
    private String valueExpression;
    @Enumerated(EnumType.STRING)
    @Column(name = "logical_operator", nullable = false, length = 4)
    private LogicalOperator logicalOperator;
    @Column(name = "parameterizable", nullable = false)
    private boolean parameterizable;
}
