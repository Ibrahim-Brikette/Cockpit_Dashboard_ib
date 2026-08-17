package com.dynamicdashboard.cockpit.identity.application.mapper;
import com.dynamicdashboard.cockpit.identity.application.dto.UserAccountDto;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import org.springframework.stereotype.Component;
@Component
public class IdentityMapper {
    public UserAccountDto toDto(UserAccountEntity entity) {
        if (entity == null) return null;
        return UserAccountDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .displayName(entity.getDisplayName())
                .accountStatus(entity.getAccountStatus() != null ? entity.getAccountStatus().name().toLowerCase() : "active")
                .lastLoginAt(entity.getLastLoginAt())
                .build();
    }
}
