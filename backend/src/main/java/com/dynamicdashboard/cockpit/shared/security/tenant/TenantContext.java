package com.dynamicdashboard.cockpit.shared.security.tenant;

import java.util.UUID;

public final class TenantContext {
    public static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private TenantContext(){

    }
    public static void setCurrentTenant(UUID tenantId){
        CURRENT_TENANT.set(tenantId);
    }
    public static UUID get(){
        UUID tenantId = CURRENT_TENANT.get();
        if(tenantId == null){
            throw new IllegalStateException("No tenant context set for the surrent request");
        }
        return tenantId ;
    }
    public static void clear(){
        CURRENT_TENANT.remove();
    }
}
