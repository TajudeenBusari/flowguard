package com.flowguard.identity_service.exception;

public class PasswordUnchangedException extends RuntimeException{
  public PasswordUnchangedException() {
    super("New password must be different from the current password");
  }
}
