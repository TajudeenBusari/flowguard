package com.flowguard.identity_service.exception;

public class UserAlreadyExistsException extends Exception {
  public UserAlreadyExistsException(String email) {
    super("User already exists with email: " + email);
  }
}
