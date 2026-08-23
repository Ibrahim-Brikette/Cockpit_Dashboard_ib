package com.dynamicdashboard.cockpit.query.domain;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.QueryJoinType;
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
@Table(name = "query_join", schema = "cockpit")
public class QueryJoinEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private DataQueryEntity query;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "left_source_id", nullable = false)
    private DataSourceEntity leftSource;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "left_field_id", nullable = false)
    private DataFieldEntity leftField;
    @Enumerated(EnumType.STRING)
    @Column(name = "join_type", nullable = false, length = 24)
    private QueryJoinType joinType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "right_source_id", nullable = false)
    private DataSourceEntity rightSource;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "right_field_id", nullable = false)
    private DataFieldEntity rightField;
}
