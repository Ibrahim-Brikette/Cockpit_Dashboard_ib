package com.dynamicdashboard.cockpit.identity.domain;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "permission", schema = "cockpit")
public class PermissionEntity extends AuditableEntity {
    @Column(name = "permission_code", nullable = false, length = 120, unique = true)
    private String permissionCode;
    @Column(name = "permission_description", length = 255)
    private String permissionDescription;
}
