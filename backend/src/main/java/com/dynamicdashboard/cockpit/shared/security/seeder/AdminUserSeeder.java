package com.dynamicdashboard.cockpit.shared.security.seeder;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.identity.domain.UserRoleAssignmentEntity;
import com.dynamicdashboard.cockpit.identity.repository.RoleRepository;
import com.dynamicdashboard.cockpit.identity.repository.UserAccountRepository;
import com.dynamicdashboard.cockpit.identity.repository.UserRoleAssignmentRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AssignmentScope;
import com.dynamicdashboard.cockpit.shared.security.CockpitAuthProperties;
import com.dynamicdashboard.cockpit.shared.security.authorization.AppRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seeds one default SUPER_ADMIN user at startup (STANDALONE mode only).
 *
 * Runs after RolePermissionSeeder (@Order(2) vs @Order(1)) so the SUPER_ADMIN
 * role is guaranteed to exist before we assign it.
 *
 * Idempotent: skips if a user with the configured username already exists.
 *
 * Default credentials (override via env vars):
 *   COCKPIT_ADMIN_USERNAME    → admin
 *   COCKPIT_ADMIN_PASSWORD    → Admin1234!
 *   COCKPIT_ADMIN_EMAIL       → admin@cockpit.local
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final CockpitAuthProperties       cockpitAuthProperties;
    private final UserAccountRepository       userAccountRepository;
    private final RoleRepository              roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder             passwordEncoder;

    @Value("${cockpit.admin.username:admin}")
    private String adminUsername;

    @Value("${cockpit.admin.email:admin@cockpit.local}")
    private String adminEmail;

    @Value("${cockpit.admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${cockpit.admin.display-name:Administrator}")
    private String adminDisplayName;

    @Override
    @Transactional
    public void run(String... args) {
        if (cockpitAuthProperties.getMode() == CockpitAuthProperties.AuthMode.INTEGRATED) {
            log.info("[AdminSeeder] INTEGRATED mode — skipping admin user seeding.");
            return;
        }

        if (userAccountRepository.findByUsername(adminUsername).isPresent()) {
            log.info("[AdminSeeder] User '{}' already exists — skipping.", adminUsername);
            return;
        }

        UserAccountEntity admin = new UserAccountEntity();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminDisplayName);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setTenantId(DEFAULT_TENANT_ID);
        admin.setFailedLoginAttempts(0);
        admin.setLockoutCount(0);
        admin.setPermanentlyLocked(false);

        UserAccountEntity saved = userAccountRepository.save(admin);
        log.info("[AdminSeeder] Created admin user: username='{}' email='{}'",
                adminUsername, adminEmail);

        roleRepository.findByRoleName(AppRole.SUPER_ADMIN.name()).ifPresentOrElse(
                superAdminRole -> {
                    UserRoleAssignmentEntity assignment = new UserRoleAssignmentEntity();
                    assignment.setUser(saved);
                    assignment.setRole(superAdminRole);
                    assignment.setAssignmentScope(AssignmentScope.GLOBAL);
                    userRoleAssignmentRepository.save(assignment);
                    log.info("[AdminSeeder] SUPER_ADMIN (GLOBAL) assigned to '{}'.", adminUsername);
                },
                () -> log.warn("[AdminSeeder] SUPER_ADMIN role not found — user created without role. " +
                        "Ensure RolePermissionSeeder ran before AdminUserSeeder.")
        );
    }
}
