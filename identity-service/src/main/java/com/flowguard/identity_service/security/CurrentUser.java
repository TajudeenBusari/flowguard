package com.flowguard.identity_service.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UUID organizationId,
        String email
) {
  public static CurrentUser fromJwt(Jwt jwt){
    return new CurrentUser(
            UUID.fromString(jwt.getSubject()),
            UUID.fromString(jwt.getClaimAsString("organizationId")),
            jwt.getClaimAsString("email")
    );
  }
}
