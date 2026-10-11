package com.tjtechy.workflowservice.exception;

public class WorkflowInvalidStateException extends RuntimeException {
  public WorkflowInvalidStateException(String message) {
    super(message);
  }
}
