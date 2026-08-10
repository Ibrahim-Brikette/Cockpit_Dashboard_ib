package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission;
import com.dynamicdashboard.cockpit.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQueryPermissionEvaluator implements PermissionEvaluator {
    private final DataQueryRepository dataQueryRepository;
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
         if(!"Query".equals(targetType)) {
            return false;
        }
        boolean hasManageAll = authentication.getAuthorities().stream()
                .anyMatch(a -> a
                        .getAuthority().equals(DashboardPermission.MANAGE_ALL.getCode()));
        if(hasManageAll) {
            return true;
        }
         boolean hasBasePermission = authentication.getAuthorities().stream()
                .anyMatch(a -> a
                        .getAuthority().equals(permission.toString()));
         if(!hasBasePermission) {
            return false;
        }
        DataQueryEntity dataQuery =  dataQueryRepository.findById(UUID.fromString(targetId.toString())).orElse(null);
        if(dataQuery == null) {
            return false;
        }
        UUID ownerId = UUID.fromString(authentication.getName());
        System.out.println("owner id "+ ownerId);
        System.out.println("authentification "+ authentication.getPrincipal().getClass());
        return dataQuery.getOwner().getId().equals(ownerId)
                && dataQuery.getTenantId().equals(TenantContext.get());
    }
}
