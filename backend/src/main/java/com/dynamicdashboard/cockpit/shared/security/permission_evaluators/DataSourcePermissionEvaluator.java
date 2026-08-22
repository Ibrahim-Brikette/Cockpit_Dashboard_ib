package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity;
import com.dynamicdashboard.cockpit.catalog.repository.DataSourceRepository;
import com.dynamicdashboard.cockpit.shared.security.authorization.AppRole;
import com.dynamicdashboard.cockpit.shared.security.authorization.DatasourcePermission;
import com.dynamicdashboard.cockpit.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataSourcePermissionEvaluator implements DomainPermissionEvaluator {
    private final DataSourceRepository dataSourceRepository;
    @Override
    public String supportedTargetType() {
        return "DataSource";
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if(!"DataSource".equals(targetType)) {
            return false ;
        }
        boolean hasManageAll = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(DatasourcePermission.MANAGE_ALL.getCode()));
        if(hasManageAll) {
            return true;
        }
        boolean tenantAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(AppRole.TENANT_ADMIN.springRole()));
        if(tenantAdmin) {
            return true;
        }
        boolean superAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(AppRole.SUPER_ADMIN.springRole()));
        if(superAdmin) {
            return true;
        }


        UUID currentUser = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        DataSourceEntity  dataSourceEntity = dataSourceRepository.findById(currentUser).orElse(null);
        if(dataSourceEntity == null) {
            return false;
        }

        boolean hasPermission = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(permission.toString()));
        return hasPermission;


    }
}
