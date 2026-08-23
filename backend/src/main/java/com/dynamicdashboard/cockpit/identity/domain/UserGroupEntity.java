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
@Table(name = "user_group", schema = "cockpit")
public class UserGroupEntity extends AuditableEntity {
    @Column(name = "group_name", nullable = false, length = 120, unique = true)
    private String groupName;
    @Column(name = "group_code", nullable = false, length = 80, unique = true)
    private String groupCode;
    @Column(name = "group_description", length = 255)
    private String groupDescription;
}
