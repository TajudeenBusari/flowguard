package com.flowguard.identity_service.exception;

import com.flowguard.identity_service.entity.Role;

public class InvalidRoleOperationException extends RuntimeException{
  public InvalidRoleOperationException(Role role) {
    super("Operation is not allowed for role: " + role);
  }
}
