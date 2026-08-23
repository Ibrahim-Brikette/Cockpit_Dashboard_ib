package com.dynamicdashboard.cockpit.shared.security.authorization;

public enum DbConnection implements AppPermission {

    /** Metadata only - Spec 6.4 is explicit credentials are never returned via this. */
    TEST("connection:test"),
    CREATE("connection:create"),
    EDIT("connection:edit"),
    DELETE("connection:delete"),
    VIEW("connection:view"),
    MANAGE_ALL("connection:manage-all");

    private final String code;

    DbConnection(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
