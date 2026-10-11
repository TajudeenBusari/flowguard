package com.tjtechy.workflowservice.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * authenticated JWT
 *       │
 *       └── organizationId
 *               ↓
 *          CurrentTenant
 *               ↓
 *             UUID
 *               ↓
 *      WorkflowService
 *               ↓
 * findAllByOrganizationId(...)
 */
public final class CurrentTenant {
  private CurrentTenant() {}

  public static UUID organizationId(Jwt jwt) {
    return UUID.fromString(jwt.getClaimAsString("organizationId"));
  }
}
