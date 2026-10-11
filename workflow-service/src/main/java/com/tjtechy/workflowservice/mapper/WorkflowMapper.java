package com.tjtechy.workflowservice.mapper;

import com.tjtechy.workflowservice.dto.WorkflowResponse;
import com.tjtechy.workflowservice.dto.WorkflowStepResponse;
import com.tjtechy.workflowservice.dto.WorkflowVersionResponse;
import com.tjtechy.workflowservice.entity.Workflow;
import com.tjtechy.workflowservice.entity.WorkflowStep;
import com.tjtechy.workflowservice.entity.WorkflowVersion;

//final → don't extend this utility class.
public final class WorkflowMapper {

  //private WorkflowMapper() → don't instantiate it.
  private WorkflowMapper() {}

  public static WorkflowResponse mapFromWorkflowToWorkflowResponseDto(Workflow workflow) {
    return new WorkflowResponse(
            workflow.getId(),
            workflow.getName(),
            workflow.getDescription(),
            workflow.getCreatedAt(),
            workflow.getUpdatedAt()
    );
  }

  public static WorkflowStepResponse mapFromWorkflowStepToWorkflowStepResponseDto(WorkflowStep workflowStep) {
    return new WorkflowStepResponse(
            workflowStep.getId(),
            workflowStep.getName(),
            workflowStep.getType(),
            workflowStep.getPosition(),
            workflowStep.getCreatedAt(),
            workflowStep.getUpdatedAt()
    );
  }

  public static WorkflowVersionResponse mapFromWorkflowVersionToWorkflowVersionResponseDto(WorkflowVersion workflowVersion) {
    return new WorkflowVersionResponse(
            workflowVersion.getId(),
            workflowVersion.getWorkflowId(),
            workflowVersion.getVersion(),
            workflowVersion.getStatus(),
            workflowVersion.getCreatedAt(),
            workflowVersion.getUpdatedAt()
    );
  }
}
