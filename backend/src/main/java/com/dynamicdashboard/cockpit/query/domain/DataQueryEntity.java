package com.dynamicdashboard.cockpit.query.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AggregationType;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryVisibility;
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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "data_query", schema = "cockpit")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DataQueryEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccountEntity owner;
    @Column(name = "query_name", nullable = false, length = 160)
    private String queryName;
    @Column(name = "query_description", length = 500)
    private String queryDescription;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 24)
    private QueryVisibility visibility;
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation", nullable = false, length = 24)
    private AggregationType aggregation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aggregation_field_id")
    private DataFieldEntity aggregationField;
    @Column(name = "row_limit", nullable = false)
    private int rowLimit;
    @Column(name = "used_by_widgets", nullable = false)
    private int usedByWidgets;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
