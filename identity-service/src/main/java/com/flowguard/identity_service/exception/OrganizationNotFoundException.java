package com.flowguard.identity_service.exception;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {
  public OrganizationNotFoundException(UUID organizationId) {
    super("Organization with id " + organizationId + " not found.");

  }
}
