package com.legalpro.accountservice.security;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Account account;

    public UUID getUuid() {
        return account.getUuid();
    }

    public Long getId() {
        return account.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Always prefix ROLE_ so Spring Security picks it up
        return account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // ✅ Factory method so JwtAuthenticationFilter can reconstruct CustomUserDetails
    public static CustomUserDetails fromJwtClaims(UUID uuid, String email, Set<String> roles) {
        Account account = new Account();
        account.setUuid(uuid);
        account.setEmail(email);
        account.setPassword(""); // no need to set password when restoring from token
        account.setRoles(
                roles.stream()
                        .map(roleName -> {
                            Role r = new Role();
                            r.setName(roleName.replace("ROLE_", "")); // store clean role name
                            return r;
                        })
                        .collect(Collectors.toSet())
        );
        return new CustomUserDetails(account);
    }
}
