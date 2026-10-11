package com.tjtechy.workflowservice.service;

import com.tjtechy.workflowservice.dto.*;
import com.tjtechy.workflowservice.entity.WorkflowVersion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WorkflowService {
  Flux<WorkflowResponse> getWorkflows(UUID organizationId);
  Mono<WorkflowResponse> createWorkflow(UUID organizationId, CreateWorkflowInput workflowInput);
  Mono<WorkflowStepResponse> addStep(UUID organizationId, UUID workflowId, CreateWorkflowStepInput input);

  /**
   *GraphQL request
   *       ↓
   * organizationId from JWT
   *       ↓
   * Verify workflow ownership
   *       ↓
   * Retrieve workflow versions
   *       ↓
   * Map to WorkflowVersionResponse
   * This prevents users from retrieving workflow versions belonging to another organization.
   */
  Flux<WorkflowVersionResponse> getWorkflowVersions(UUID organizationId, UUID workflowId);

  /**
   * Why do we need organizationId?
   * Because FlowGuard is multi-tenant, we must ensure that the requested
   * workflow version belongs to a workflow owned by the authenticated organization
   * GraphQL workflowSteps(workflowVersionId)
   *              ↓
   * Extract organizationId from JWT
   *              ↓
   * Verify workflow version belongs to tenant
   *              ↓
   * Retrieve steps ordered by position
   *              ↓
   * Return WorkflowStepResponse
   */
  Flux<WorkflowStepResponse> getWorkflowSteps(UUID organizationId, UUID workflowVersionId);

  Mono<WorkflowVersionResponse> publishWorkflow(UUID organizationId, UUID workflowId);

  Mono<WorkflowVersionResponse> createDraftVersion(UUID organizationId, UUID workflowId);
}
