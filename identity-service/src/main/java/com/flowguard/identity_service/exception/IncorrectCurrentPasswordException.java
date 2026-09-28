package com.flowguard.identity_service.exception;

public class IncorrectCurrentPasswordException extends RuntimeException{
  public IncorrectCurrentPasswordException(){
    super("Current password is incorrect");
  }
}
