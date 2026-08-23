package com.dynamicdashboard.cockpit.shared.security.permission_evaluators;

import com.dynamicdashboard.cockpit.query.domain.DataQueryEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.shared.security.authorization.AppRole;
import com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission;
import com.dynamicdashboard.cockpit.sharing.repository.QueryShareGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

// Old: implemented PermissionEvaluator directly — exposed this class to Spring Security's type
// system, meaning it could accidentally be set as THE evaluator, bypassing the delegator entirely.
// The compiler would not catch that mistake. Also used the deleted sharing.repository import.
// public class DataQueryPermissionEvaluator implements PermissionEvaluator {
@Component
@RequiredArgsConstructor

public class DataQueryPermissionEvaluator implements DomainPermissionEvaluator {
    private final DataQueryRepository dataQueryRepository;
    private final  QueryShareGrantRepository queryShareGrantRepository;

    @Override
    public String supportedTargetType() {
        return "Query";
    }

    // Old: required by PermissionEvaluator contract — object-based overload, always returned false
    // because this evaluator only supports ID-based checks (the other overload below).
    // @Override
    // public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    //     return false;
    // }

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
                .anyMatch(a -> a.getAuthority().equals(AppRole.TENANT_ADMIN.springRole()));

        boolean superAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AppRole.SUPER_ADMIN.springRole()));

        if(tenantAdmin || superAdmin) {
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
        if (dataQuery.getOwner() != null && dataQuery.getOwner().getId().equals(currentUserId)){
            return true;
        }
        Integer grantedRank = queryShareGrantRepository.findMaxAccessLevelRank(dataQuery.getId(), currentUserId);
        if (grantedRank == null) {
            return false;
        }

        return grantedRank >= requiredAccessLevelRank(permission.toString());
    }
    private int requiredAccessLevelRank(String permissionCode) {
        if (permissionCode.equals(QueryPermission.DELETE.getCode()) ||
            permissionCode.equals(QueryPermission.SHARE.getCode())) {
            return 3; // OWNER-tier share required — only owner or OWNER-level grantee may share or delete
        }
        if (permissionCode.equals(QueryPermission.EDIT.getCode())) {
            return 2; // EDIT-tier share required
        }
        return 1; // VIEW, EXECUTE — READ-tier is enough
    }
}
