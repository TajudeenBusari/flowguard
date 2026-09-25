package com.flowguard.identity_service.security;

import com.flowguard.identity_service.entity.UserRole;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowGuardUserDetailsService {
  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;

  /**
   * Load user by organization ID and email, returning a FlowGuardPrincipal with associated roles.
   *why BadCredentialsException instead of organization doesn't exist, user doesn't exist
   * or email doesn't exist?, the login API shouldn't
   * reveal which part of the credentials is invalid to prevent user enumeration attacks.
   */
  public Mono<FlowGuardPrincipal> loadUser(UUID organizationId, String email) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    return userRepository.findByOrganizationIdAndEmail(organizationId, normalizedEmail)
            .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid email or password")))
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole)
                    .collect(Collectors.toSet())
                    .map(roles -> new FlowGuardPrincipal(user, roles))
            );
  }
}
