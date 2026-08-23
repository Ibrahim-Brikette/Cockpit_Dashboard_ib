package com.dynamicdashboard.cockpit.identity.domain;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.MembershipRole;
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
@Table(name = "user_group_membership", schema = "cockpit")
public class UserGroupMembershipEntity extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroupEntity group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_role", nullable = false, length = 24)
    private MembershipRole membershipRole;
}
