package com.flowguard.identity_service.exception;

/**
 * Remember, FlowGuard permits the same email in different organizations;
 * the uniqueness rule is (organization_id, email)
 */
public class EmailAlreadyExistsException extends RuntimeException{
  public EmailAlreadyExistsException(String email){
    super("Email already exists: " + email);
  }
}
