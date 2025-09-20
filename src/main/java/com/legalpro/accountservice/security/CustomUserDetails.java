package com.legalpro.accountservice.security;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Account account;
    private UUID uuidFromToken; // ✅ mutable UUID from JWT

    public CustomUserDetails(Account account) {
        this.account = account;
        this.uuidFromToken = account.getUuid(); // default to DB UUID
    }

    public UUID getUuid() {
        return uuidFromToken;
    }

    public void setUuidFromToken(UUID uuidFromToken) {
        this.uuidFromToken = uuidFromToken;
    }

    public Long getId() {
        return account.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() { return account.getPassword(); }

    @Override
    public String getUsername() { return account.getEmail(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // ✅ Factory method to reconstruct CustomUserDetails from JWT claims
    public static CustomUserDetails fromJwtClaims(UUID uuid, String email, Set<String> roles) {
        Account account = new Account();
        account.setUuid(uuid);
        account.setEmail(email);
        account.setPassword(""); // password not needed
        account.setRoles(
                roles.stream()
                        .map(roleName -> {
                            Role r = new Role();
                            r.setName(roleName.replace("ROLE_", "")); // remove ROLE_ prefix
                            return r;
                        })
                        .collect(Collectors.toSet())
        );

        CustomUserDetails userDetails = new CustomUserDetails(account);
        userDetails.setUuidFromToken(uuid); // ensure UUID comes from token
        return userDetails;
    }
}
