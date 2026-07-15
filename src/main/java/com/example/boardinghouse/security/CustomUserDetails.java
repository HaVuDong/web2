package com.example.boardinghouse.security;

import com.example.boardinghouse.domain.entity.Tenant;
import com.example.boardinghouse.domain.entity.User;
import com.example.boardinghouse.domain.enums.TenantStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    @Getter
    private final User user;
    
    @Getter
    private final Tenant tenant;

    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isActive;

    public CustomUserDetails(User user) {
        this.user = user;
        this.tenant = null;
        this.username = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        this.isActive = user.isActive();
    }

    public CustomUserDetails(Tenant tenant) {
        this.user = null;
        this.tenant = tenant;
        this.username = tenant.getEmail();
        this.password = tenant.getPasswordHash();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_TENANT"));
        this.isActive = tenant.getStatus() == TenantStatus.ACTIVE && !Boolean.TRUE.equals(tenant.getDeleted());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
