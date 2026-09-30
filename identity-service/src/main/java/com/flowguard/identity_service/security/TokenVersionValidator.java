package com.flowguard.identity_service.security;

import com.flowguard.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;


/**
 * This method compares the JWT's tokenVersion with the current database value.
 * JWT tokenVersion = 0
 *           ↓
 * load user from database
 *           ↓
 * DB tokenVersion = 0 → valid
 * DB tokenVersion = 1 → invalid
 * TokenVersionValidator has to be injected into the security filter chain to validate
 * the token version on every request.
 * So, matching the RSA signature alone is no longer sufficient. Version mismatch will result in a 401 Unauthorized response.
 */
//@Component
//@RequiredArgsConstructor
//public class TokenVersionValidator {
//
//  private final UserRepository userRepository;
//
//  public Mono<Boolean> isTokenVersionValid(Jwt jwt){
//
//    UUID userId = UUID.fromString(jwt.getSubject());
//    UUID organizationId = UUID.fromString(jwt.getClaimAsString("organizationId"));
//    Integer tokenVersion = jwt.getClaim("tokenVersion");
//
//    return userRepository.findByIdAndOrganizationId(userId, organizationId)
//            .map(user ->
//                    Objects.equals(user.getTokenVersion(), tokenVersion)).defaultIfEmpty(false);
//
//  }
//}
