package com.dynamicdashboard.cockpit.query.domain;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserGroupEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccessLevel;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "query_share_grant", schema = "cockpit")
public class QueryShareGrantEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private DataQueryEntity query;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_level", nullable = false, length = 24)
    private ShareLevel shareLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_user_id")
    private UserAccountEntity granteeUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_group_id")
    private UserGroupEntity granteeGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 24)
    private AccessLevel accessLevel;
}