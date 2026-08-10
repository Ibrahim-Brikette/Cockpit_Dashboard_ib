package com.dynamicdashboard.cockpit.shared.security.authorization;

public enum DashboardPermission implements AppPermission {

    VIEW("dashboard:view"),
    CREATE("dashboard:create"),
    EDIT("dashboard:edit"),
    DELETE("dashboard:delete"),
    SHARE("dashboard:share"),
    /** TENANT_ADMIN's "all dashboards within tenant" - manage others' dashboards, not just own (6.1). */
    MANAGE_ALL("dashboard:manage_all");

    private final String code;

    DashboardPermission(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
