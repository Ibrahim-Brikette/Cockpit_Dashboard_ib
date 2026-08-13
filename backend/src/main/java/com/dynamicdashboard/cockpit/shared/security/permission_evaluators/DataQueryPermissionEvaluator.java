package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.query.repository.QueryShareGrantRepository;
import com.dynamicdashboard.cockpit.shared.security.authorization.DashboardPermission;
import com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission;
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
    private final QueryShareGrantRepository queryShareGrantRepository;

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
                        .getAuthority().equals(QueryPermission.MANAGE_ALL.getCode()));
        if(hasManageAll) {
            return true;
        }
        boolean tenantAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a
                        .getAuthority().equals("ROLE_TENANT_ADMIN"));

        boolean systemAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a
                        .getAuthority().equals("ROLE_SYSTEM_ADMIN"));
        if(tenantAdmin || systemAdmin) {
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
        UUID currentUserId = UUID.fromString(authentication.getName());
        if (dataQuery.getOwner().getId().equals(currentUserId)){
            return true;
        }
        Integer grantedRank = queryShareGrantRepository.findMaxAccessLevelRank(dataQuery.getId(), currentUserId);
        if (grantedRank == null) {
            return false;
        }

        return grantedRank >= requiredAccessLevelRank(permission.toString());
    }
    private int requiredAccessLevelRank(String permissionCode) {
        if (permissionCode.equals(QueryPermission.DELETE.getCode())) {
            return 3; // OWNER-tier share required
        }
        if (permissionCode.equals(QueryPermission.EDIT.getCode())) {
            return 2; // EDIT-tier share required
        }
        return 1; // VIEW, EXECUTE - READ-tier is enough (judgment call - see below)
    }
}
