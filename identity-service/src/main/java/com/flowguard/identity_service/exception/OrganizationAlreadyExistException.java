package com.flowguard.identity_service.exception;

public class OrganizationAlreadyExistException extends RuntimeException {
  public OrganizationAlreadyExistException(String slug) {
    super("Organization with slug '" + slug + "' already exists.");
  }
}
