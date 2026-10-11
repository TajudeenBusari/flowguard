package com.tjtechy.workflowservice.exception;

public class WorkflowNotFoundException extends RuntimeException {
  public WorkflowNotFoundException(String message) {
    super(message);
  }
}
