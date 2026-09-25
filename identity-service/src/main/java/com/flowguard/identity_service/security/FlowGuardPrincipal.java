package com.flowguard.identity_service.security;

import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class FlowGuardPrincipal implements UserDetails {

  private final User user;
  private final Set<Role> roles;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();
  }

  @Override
  public @Nullable String getPassword() {
    return user.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return user.getStatus() != UserStatus.SUSPENDED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return user.getStatus() == UserStatus.ACTIVE;
  }
}
