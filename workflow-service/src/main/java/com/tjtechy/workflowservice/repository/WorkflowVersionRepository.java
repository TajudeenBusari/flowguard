package com.tjtechy.workflowservice.repository;

import com.tjtechy.workflowservice.entity.WorkflowVersion;
import com.tjtechy.workflowservice.entity.WorkflowVersionStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WorkflowVersionRepository extends ReactiveCrudRepository<WorkflowVersion, UUID> {
  Mono<WorkflowVersion> findByWorkflowIdAndStatus(UUID workflowId, WorkflowVersionStatus status);

  /**
   * Why FOR UPDATE?
   * When this query executes inside a database transaction,
   * PostgreSQL locks the selected draft-version row until the transaction completes.
   * This will be used in the addStep() method of WorkflowService to ensure that only one
   * step is added to the draft version at a time.
   */
  @Query("""
          SELECT *
          FROM workflow_versions
          WHERE workflow_id= :workflowId
            AND status= 'DRAFT'
          FOR UPDATE
          """)
  Mono<WorkflowVersion> findDraftVersionForUpdate(@Param("workflowId") UUID workflowId);

  Flux<WorkflowVersion> findAllByWorkflowIdOrderByVersionDesc(UUID workflowId);

  /**
   * Why use a JOIN?
   * The workflow_versions table doesn't contain organization_id. That information is stored in the workflows table.
   * The JOIN allows PostgreSQL to verify ownership before returning the version.
   * If the version doesn't exist or belongs to another organization, the repository returns Mono.empty().
   */
  @Query("""
          SELECT *
          FROM workflow_versions wv
          INNER JOIN workflows w
              ON wv.workflow_id = w.id
          WHERE wv.id = :workflowVersionId
            AND w.organization_id = :organizationId
          """)
  Mono<WorkflowVersion> findByIdAndOrganizationId(@Param("workflowVersionId") UUID workflowVersionId, @Param("organizationId") UUID organizationId);

  /**
   * Why do we need this?
   * When creating a new draft version, FlowGuard needs to determine the next version number.
   * For example:
   * Existing latest version         New version
   * 1 (PUBLISHED)                          2 (DRAFT)
   * 2 (PUBLISHED)                          3 (DRAFT)
   * 3 (RETIRED)                           4 (DRAFT)
   */
  Mono<WorkflowVersion> findFirstByWorkflowIdOrderByVersionDesc(UUID workflowId);

  /**
   *This will let us find and lock the currently published version so we can change its status to RETIRED.
   * The intended lifecycle is:
   * Before publishing version 2:
   * Version 1 → PUBLISHED
   * Version 2 → DRAFT

   * After publishing version 2:
   * Version 1 → RETIRED
   * Version 2 → PUBLISHED
   * We'll perform both status updates in a single transaction so they either both succeed or both roll back.
   */
  @Query("""
          SELECT *
          FROM workflow_versions
          WHERE workflow_id= :workflowId
            AND status= 'PUBLISHED'
          FOR UPDATE
          """)
  Mono<WorkflowVersion> findPublishedVersionForUpdate(@Param("workflowId") UUID workflowId);
}
