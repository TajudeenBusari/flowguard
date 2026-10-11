package com.tjtechy.workflowservice.repository;

import com.tjtechy.workflowservice.entity.Workflow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WorkflowRepository extends ReactiveCrudRepository<Workflow, UUID> {
  Flux<Workflow> findAllByOrganizationId(UUID organizationId);
  Mono<Workflow> findByIdAndOrganizationId(UUID id, UUID organizationId);

  /**
   * Suppose two requests attempt to create versions simultaneously.
   * Without locking, both could read version1 as the latest version and create version2, resulting in two version2s.
   * With FOR UPDATE, the first request locks the row for version1, preventing the second request from reading it until the first transaction completes,
   * ensuring that the second request sees version2 as the latest version and creates version3 instead of a duplicate version2.
   * This prevents race conditions and ensures that version numbers are unique and sequential.
   * This complements the existing PostgreSQL unique constraints.
   */
  @Query("""
          SELECT *
          FROM workflows
          WHERE id = :workflowId
            AND organization_id = :organizationId
          FOR UPDATE
          """)
  Mono<Workflow> findByIdAndOrganizationIdForUpdate(UUID workflowId, UUID organizationId);
}
