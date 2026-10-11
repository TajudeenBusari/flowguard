package com.tjtechy.workflowservice.controller;

import com.tjtechy.workflowservice.dto.*;
import com.tjtechy.workflowservice.security.CurrentTenant;
import com.tjtechy.workflowservice.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WorkflowMutationController {

  private final WorkflowService workflowService;

  @MutationMapping
  public Mono<WorkflowResponse> createWorkflow(
          @Argument @Valid CreateWorkflowInput input,
          @AuthenticationPrincipal Jwt jwt){
    return workflowService.createWorkflow(CurrentTenant.organizationId(jwt), input);
  }

  @MutationMapping
  public Mono<WorkflowStepResponse> addStepToWorkflow(
          @Argument UUID workflowId,
          @Argument @Valid CreateWorkflowStepInput input,
          @AuthenticationPrincipal Jwt jwt){
    return workflowService.addStep(CurrentTenant.organizationId(jwt), workflowId, input);
  }

  /**
   * GraphQL publishWorkflow(workflowId)
   *                 ↓
   * Extract organizationId from JWT
   *                 ↓
   * WorkflowService.publishWorkflow(...)
   *                 ↓
   * Verify tenant ownership
   *                 ↓
   * Lock DRAFT version
   *                 ↓
   * Validate at least one step exists
   *                 ↓
   * Update status to PUBLISHED
   *                 ↓
   * Return WorkflowVersionResponse
   */
  @MutationMapping
  public Mono<WorkflowVersionResponse> publishWorkflow(
          @Argument UUID workflowId,
          @AuthenticationPrincipal Jwt jwt){
    return workflowService.publishWorkflow(CurrentTenant.organizationId(jwt), workflowId);
  }

  @MutationMapping
  public Mono<WorkflowVersionResponse> createDraftVersion(
          @Argument UUID workflowId,
          @AuthenticationPrincipal Jwt jwt){
    return workflowService.createDraftVersion(CurrentTenant.organizationId(jwt), workflowId);
  }

}
