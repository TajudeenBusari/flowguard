package com.flowguard.identity_service.exception;

public class LastActiveOwnerException extends RuntimeException {
  public LastActiveOwnerException(){
    super("Organization must have at least one active owner. Cannot remove the last active OWNER.");
  }
}
