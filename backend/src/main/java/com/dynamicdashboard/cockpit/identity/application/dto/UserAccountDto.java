package com.dynamicdashboard.cockpit.identity.application.dto;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountDto {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String accountStatus;
    private Instant lastLoginAt;
}
