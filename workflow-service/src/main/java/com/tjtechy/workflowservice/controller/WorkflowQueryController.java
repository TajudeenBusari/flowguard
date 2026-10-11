package com.tjtechy.workflowservice.controller;


import com.tjtechy.workflowservice.dto.WorkflowResponse;
import com.tjtechy.workflowservice.dto.WorkflowStepResponse;
import com.tjtechy.workflowservice.dto.WorkflowVersionResponse;
import com.tjtechy.workflowservice.security.CurrentTenant;
import com.tjtechy.workflowservice.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WorkflowQueryController {
  private final WorkflowService workflowService;

  /**
   * GraphQL has its own standardize return object, so we don't need to wrap the response in a
   * Result object like we do for REST endpoints.
   * This method retrieves all workflows for the authenticated user's organization.
   * JWT → validation → organizationId → repository tenant filter → GraphQL
   */
  @QueryMapping
  public Flux<WorkflowResponse> workflows(@AuthenticationPrincipal Jwt jwt){
    return workflowService.getWorkflows(CurrentTenant.organizationId(jwt));
  }

  @QueryMapping
  public Flux<WorkflowVersionResponse> workflowVersions(@Argument UUID workflowId, @AuthenticationPrincipal Jwt jwt){
    return workflowService.getWorkflowVersions(CurrentTenant.organizationId(jwt), workflowId);
  }

  /**
   *GraphQL workflowSteps(workflowVersionId)
   *                 ↓
   * WorkflowQueryController
   *                 ↓
   * Extract organizationId from JWT
   *                 ↓
   * WorkflowService.getWorkflowSteps(...)
   *                 ↓
   * Verify workflow version belongs to tenant
   *                 ↓
   * Return steps ordered by position
   */
  @QueryMapping
  public Flux<WorkflowStepResponse> workflowSteps(@Argument UUID workflowVersionId, @AuthenticationPrincipal Jwt jwt){
    return workflowService.getWorkflowSteps(CurrentTenant.organizationId(jwt), workflowVersionId);
  }
}
