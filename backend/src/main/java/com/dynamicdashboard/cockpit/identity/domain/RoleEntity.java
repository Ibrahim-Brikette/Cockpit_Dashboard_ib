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
@Table(name = "role", schema = "cockpit")
public class RoleEntity extends AuditableEntity {
    @Column(name = "role_name", nullable = false, length = 80, unique = true)
    private String roleName;
    @Column(name = "role_description", length = 255)
    private String roleDescription;
    @Column(name = "system_role", nullable = false)
    private boolean systemRole;
}
