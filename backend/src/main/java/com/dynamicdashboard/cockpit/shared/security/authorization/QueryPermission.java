package com.dynamicdashboard.cockpit.shared.security.authorization;

public enum QueryPermission implements AppPermission {

    VIEW("query:view"),
    CREATE("query:create"),
    EDIT("query:edit"),
    DELETE("query:delete"),
    EXECUTE("query:execute"),
    SHARE("query:share"),
    MANAGE_ALL("query:manage_all");

    private final String code;

    QueryPermission(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}

