package com.tjtechy.workflowservice.repository;

import com.tjtechy.workflowservice.entity.WorkflowStep;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WorkflowStepRepository extends ReactiveCrudRepository<WorkflowStep, UUID> {
  /**
   * When adding a step, we need to know where it belongs in the workflow.
   * This method retrieves all steps for a given workflow version,
   * ordered by their position, so we can determine the correct placement for the new step.
   */
  Flux<WorkflowStep> findAllByWorkflowVersionIdOrderByPositionAsc(UUID workflowVersionId);

  Mono<Boolean> existsByWorkflowVersionId(UUID workflowVersionId);

}
