package com.dynamicdashboard.cockpit.shared.security.authorization;

public enum DatasourcePermission implements AppPermission {

    /** Metadata only - Spec 6.4 is explicit credentials are never returned via this. */
    VIEW("datasource:view"),
    CREATE("datasource:create"),
    EDIT("datasource:edit"),
    MANAGE_ALL("datasource:manage-all"),
    GET_FIELDS("datasource:get-fields"),
    //,
//    DELETE("datasource:delete"),
//    /** TENANT_ADMIN managing datasources it doesn't own (6.1). */
//    MANAGE_ALL("datasource:manage_all"),
//    TEST_CONNECTION("datasource:test_connection")
     ;

    private final String code;

    DatasourcePermission(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}