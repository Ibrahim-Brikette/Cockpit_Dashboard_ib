package com.dynamicdashboard.cockpit.identity.domain;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
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

    // ---- Brute-force lockout fields (managed by BruteForceService) -----------

    /** Incremented on each wrong password; reset to 0 when a lockout triggers or login succeeds. */
    @ColumnDefault("0")
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    /**
     * Set to now() + lockout duration when a timed lockout triggers.
     * Null means not currently brute-force locked.
     * BruteForceService clears this on auto-expiry but keeps lockoutCount intact.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * How many times this account has been locked by brute-force.
     * Survives auto-expiry — the attacker cannot erase it by waiting.
     * Drives the escalating duration: 1→30min, 2→2h, 3→8h, 4+→permanent.
     * Reset to 0 only on successful password reset or successful login.
     */
    @ColumnDefault("0")
    @Column(name = "lockout_count", nullable = false)
    private int lockoutCount;

    /**
     * True after the 4th lockout. No auto-expiry applies.
     * The only way out is a password reset via the email link.
     * Field named "permanentlyLocked" (not "isPermanentlyLocked") so Lombok generates
     * isPermanentlyLocked() / setPermanentlyLocked() without the double-is prefix.
     */
    @ColumnDefault("false")
    @Column(name = "is_permanently_locked", nullable = false)
    private boolean permanentlyLocked;
}
