package com.dynamicdashboard.cockpit.shared.security;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final UserAccountEntity userAccount;

    public CustomUserDetails(UserAccountEntity userAccount) {
        this.userAccount = userAccount;
    }

    public UserAccountEntity getUserAccountEntity() {
        return userAccount;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // not used - authorities come from the JWT on subsequent requests
    }

    @Override
    public String getPassword() {
        return userAccount.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userAccount.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        // Old: !"LOCKED".equals(userAccount.getAccountStatus())
        // Bug: compared String to AccountStatus enum — always returned true (never locked anyone)
        return AccountStatus.LOCKED != userAccount.getAccountStatus();
    }

    @Override
    public boolean isEnabled() {
        // Old: "ACTIVE".equals(userAccount.getAccountStatus())
        // Bug: compared String to AccountStatus enum — always returned false (blocked every login)
        return AccountStatus.ACTIVE == userAccount.getAccountStatus();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
