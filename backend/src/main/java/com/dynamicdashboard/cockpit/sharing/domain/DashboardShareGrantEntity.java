package com.dynamicdashboard.cockpit.sharing.domain;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.identity.domain.RoleEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserGroupEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccessLevel;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
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
@Table(name = "dashboard_share_grant", schema = "cockpit")
public class DashboardShareGrantEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private DashboardEntity dashboard;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_role_id")
    private RoleEntity granteeRole;
}
