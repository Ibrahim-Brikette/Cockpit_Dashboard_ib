package com.dynamicdashboard.cockpit.shared.security;

import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
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
        return !"LOCKED".equals(userAccount.getAccountStatus());
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(userAccount.getAccountStatus());
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
