package com.dynamicdashboard.cockpit.identity.domain;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "user_account", schema = "cockpit")
public class UserAccountEntity extends AuditableEntity {
    @Column(name = "username", nullable = false, length = 80, unique = true)
    private String username;
    @Column(name = "email", nullable = false, length = 180, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 24)
    private AccountStatus accountStatus;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
