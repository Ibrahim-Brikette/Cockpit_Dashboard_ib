package com.dynamicdashboard.cockpit.identity.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "role_permission", schema = "cockpit")
public class RolePermissionEntity {
    @EmbeddedId
    private RolePermissionId id = new RolePermissionId();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class RolePermissionId implements Serializable {
        @Column(name = "role_id", nullable = false)
        private UUID roleId;
        @Column(name = "permission_id", nullable = false)
        private UUID permissionId;
    }
}
