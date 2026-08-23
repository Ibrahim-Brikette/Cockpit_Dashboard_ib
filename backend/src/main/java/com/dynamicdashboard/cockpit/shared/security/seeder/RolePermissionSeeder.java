package com.dynamicdashboard.cockpit.shared.security.seeder;

import com.dynamicdashboard.cockpit.identity.domain.PermissionEntity;
import com.dynamicdashboard.cockpit.identity.domain.RoleEntity;
import com.dynamicdashboard.cockpit.identity.domain.RolePermissionEntity;
import com.dynamicdashboard.cockpit.identity.repository.PermissionRepository;
import com.dynamicdashboard.cockpit.identity.repository.RolePermissionRepository;
import com.dynamicdashboard.cockpit.identity.repository.RoleRepository;
import com.dynamicdashboard.cockpit.shared.security.CockpitAuthProperties;
import com.dynamicdashboard.cockpit.shared.security.authorization.AppRole;
import com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission;
import com.dynamicdashboard.cockpit.shared.security.authorization.DatasourcePermission;
import com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Seeds the role and permission tables at startup (STANDALONE mode only).
 *
 * INTEGRATED mode: skipped entirely — the mother app owns identity/roles.
 * STANDALONE mode: for each AppRole, find-or-create the role row, find-or-create
 *   each permission row, and link them in role_permission if not already linked.
 *
 * Purely additive — never deletes or overwrites existing data.
 * Safe to run on every startup: all operations are idempotent.
 *
 * To add a new role or permission:
 *   1. Add the constant to AppRole.java (backend) and app-role.enum.ts (frontend).
 *   2. Add its permission codes to ROLE_PERMISSIONS below.
 *   3. No migration needed — this seeder creates the rows automatically.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RolePermissionSeeder implements CommandLineRunner {

    private final CockpitAuthProperties    cockpitAuthProperties;
    private final RoleRepository           roleRepository;
    private final PermissionRepository     permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // ── Role → permission codes mapping ──────────────────────────────────────

    private static final Map<AppRole, List<String>> ROLE_PERMISSIONS = Map.of(
        AppRole.SUPER_ADMIN, List.of(
            DashboardPermission.MANAGE_ALL.getCode(),
            QueryPermission.MANAGE_ALL.getCode(),
            DatasourcePermission.MANAGE_ALL.getCode()
        ),
        AppRole.TENANT_ADMIN, List.of(
            DashboardPermission.MANAGE_ALL.getCode(),
            QueryPermission.MANAGE_ALL.getCode(),
            DatasourcePermission.VIEW.getCode()
        ),
        AppRole.DATASOURCE_MANAGER, List.of(
            DatasourcePermission.CREATE.getCode(),
            DatasourcePermission.EDIT.getCode(),
            DatasourcePermission.VIEW.getCode(),
            DatasourcePermission.GET_FIELDS.getCode(),
            QueryPermission.CREATE.getCode(),
            QueryPermission.EDIT.getCode()
        ),
        AppRole.DASHBOARD_CREATOR, List.of(
            DashboardPermission.CREATE.getCode(),
            DashboardPermission.EDIT.getCode(),
            DashboardPermission.DELETE.getCode(),
            DashboardPermission.SHARE.getCode(),
            QueryPermission.CREATE.getCode(),
            QueryPermission.EDIT.getCode(),
            QueryPermission.DELETE.getCode(),
            QueryPermission.EXECUTE.getCode(),
            QueryPermission.SHARE.getCode()
        ),
        AppRole.DASHBOARD_VIEWER, List.of(
            DashboardPermission.VIEW.getCode()
        ),
        AppRole.AI_USER, List.of(),   // Add-on role — no standalone permissions
        AppRole.READONLY_ANALYST, List.of(
            QueryPermission.EXECUTE.getCode(),
            DashboardPermission.VIEW.getCode()
        )
    );

    private static final Map<AppRole, String> ROLE_DESCRIPTIONS = Map.of(
        AppRole.SUPER_ADMIN,        "Full system access. Max 2 accounts. Hardware MFA required.",
        AppRole.TENANT_ADMIN,       "Manages tenant users, datasources, and all dashboards within the tenant. MFA required.",
        AppRole.DATASOURCE_MANAGER, "Create/edit/delete datasources and query templates. Cannot view credentials in plaintext after save.",
        AppRole.DASHBOARD_CREATOR,  "Create/edit/delete own dashboards and queries; share dashboards. Cannot create datasources.",
        AppRole.DASHBOARD_VIEWER,   "View shared dashboards only. Cannot edit. Default role for new users.",
        AppRole.AI_USER,            "Add-on role: AI SQL generation; subject to rate limits. Requires a base role.",
        AppRole.READONLY_ANALYST,   "Execute existing queries; view dashboards; no create/edit rights."
    );

    // ── Runner ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(String... args) {
        if (cockpitAuthProperties.getMode() == CockpitAuthProperties.AuthMode.INTEGRATED) {
            log.info("[Seeder] INTEGRATED mode — skipping role/permission seeding (mother app owns identity).");
            return;
        }

        log.info("[Seeder] STANDALONE mode — seeding roles and permissions...");

        ROLE_PERMISSIONS.forEach((appRole, codes) -> {
            RoleEntity role = findOrCreateRole(appRole);
            codes.forEach(code -> linkIfAbsent(role, findOrCreatePermission(code)));
        });

        log.info("[Seeder] Role/permission seeding complete.");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RoleEntity findOrCreateRole(AppRole appRole) {
        return roleRepository.findByRoleName(appRole.name()).orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setRoleName(appRole.name());
            r.setSystemRole(true);
            r.setRoleDescription(ROLE_DESCRIPTIONS.get(appRole));
            log.info("[Seeder] Created role: {}", appRole.name());
            return roleRepository.save(r);
        });
    }

    private PermissionEntity findOrCreatePermission(String code) {
        return permissionRepository.findByPermissionCode(code).orElseGet(() -> {
            PermissionEntity p = new PermissionEntity();
            p.setPermissionCode(code);
            log.info("[Seeder] Created permission: {}", code);
            return permissionRepository.save(p);
        });
    }

    private void linkIfAbsent(RoleEntity role, PermissionEntity permission) {
        RolePermissionEntity.RolePermissionId id = new RolePermissionEntity.RolePermissionId();
        id.setRoleId(role.getId());
        id.setPermissionId(permission.getId());
        if (!rolePermissionRepository.existsById(id)) {
            RolePermissionEntity link = new RolePermissionEntity();
            link.setRole(role);
            link.setPermission(permission);
            rolePermissionRepository.save(link);
            log.debug("[Seeder] Linked {} → {}", role.getRoleName(), permission.getPermissionCode());
        }
    }
}
