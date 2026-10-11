package com.tjtechy.workflowservice.service.Impl;

import com.tjtechy.workflowservice.dto.*;
import com.tjtechy.workflowservice.entity.*;
import com.tjtechy.workflowservice.exception.WorkflowInvalidStateException;
import com.tjtechy.workflowservice.exception.WorkflowNotFoundException;
import com.tjtechy.workflowservice.mapper.WorkflowMapper;
import com.tjtechy.workflowservice.repository.WorkflowRepository;
import com.tjtechy.workflowservice.repository.WorkflowStepRepository;
import com.tjtechy.workflowservice.repository.WorkflowVersionRepository;
import com.tjtechy.workflowservice.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

  private final WorkflowRepository workflowRepository;
  private final WorkflowVersionRepository workflowVersionRepository;
  private final TransactionalOperator transactionalOperator;
  private final WorkflowStepRepository workflowStepRepository;


  /**
   * findAll() will cross the tenant boundary and return all workflows from all organizations.
   * So,we have to make the service method tenant aware by passing the organizationId to
   * the repository method findAllByOrganizationId().
   */
  @Override
  public Flux<WorkflowResponse> getWorkflows(UUID organizationId) {
    return workflowRepository.findAllByOrganizationId(organizationId)
            .map(WorkflowMapper::mapFromWorkflowToWorkflowResponseDto);
  }

  /**
   * createWorkflow() will create a new workflow and its first version in a single transaction.
   * The transactionalOperator.transactional() method will ensure that if any of the save operations fail,
   * the entire transaction will be rolled back and no data will be persisted to the database.
   * Every newly created workflow starts with Version 1/Draft and both records are created in a single transaction to ensure data consistency.
   */
  @Override
  public Mono<WorkflowResponse> createWorkflow(UUID organizationId, CreateWorkflowInput workflowInput) {
    Instant now = Instant.now();
    Workflow workflow = Workflow.builder()
            .organizationId(organizationId)
            .name(workflowInput.name())
            .description(workflowInput.description())
            .createdAt(now)
            .updatedAt(now)
            .build();
    return transactionalOperator.transactional(
            workflowRepository
                    .save(workflow)
                    .flatMap(savedWorkflow -> {
                      WorkflowVersion workflowVersion = WorkflowVersion.builder()
                              .workflowId(savedWorkflow.getId())
                              .version(1)
                              .status(WorkflowVersionStatus.DRAFT)
                              .createdAt(now)
                              .updatedAt(now)
                              .build();
                      return workflowVersionRepository.save(workflowVersion)
                              .thenReturn(savedWorkflow);
                    })
                    .map(WorkflowMapper::mapFromWorkflowToWorkflowResponseDto));
  }

  /**
   * JWT organizationId
   *       +
   * workflowId
   *       ↓
   * tenant-owned workflow?
   *       ↓ yes
   * DRAFT version exists?
   *       ↓ yes
   * safe to modify draft
   */
  @Override
  public Mono<WorkflowStepResponse> addStep(UUID organizationId,
                                            UUID workflowId,
                                            CreateWorkflowStepInput input) {
    return transactionalOperator.transactional (
            workflowRepository
                    .findByIdAndOrganizationId(workflowId, organizationId)
                    .switchIfEmpty(Mono.error(new WorkflowNotFoundException("Workflow not found")))
                    .flatMap(workflow -> workflowVersionRepository.findDraftVersionForUpdate(workflow.getId())
                            .switchIfEmpty(Mono.error(new WorkflowInvalidStateException("Workflow has no draft version"))))
                    .flatMap(draftVersion ->
                        //step create here
                        workflowStepRepository.findAllByWorkflowVersionIdOrderByPositionAsc(draftVersion.getId())
                      .collectList()
                      .flatMap(existingSteps -> {
                        int nextPosition = existingSteps.size() + 1;
                        Instant now = Instant.now();
                        WorkflowStep step = WorkflowStep.builder()
                                .workflowVersionId(draftVersion.getId())
                                .name(input.name().trim())
                                .type(WorkflowStepType.TASK)
                                .position(nextPosition)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        return workflowStepRepository.save(step);
                      }).map(WorkflowMapper::mapFromWorkflowStepToWorkflowStepResponseDto))
            );
  }

  @Override
  public Flux<WorkflowVersionResponse> getWorkflowVersions(UUID organizationId, UUID workflowId) {

    return workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)

            .switchIfEmpty(Mono.error(
                    new WorkflowNotFoundException("Workflow not found")))

            .flatMapMany(workflow ->
                    workflowVersionRepository.findAllByWorkflowIdOrderByVersionDesc(workflow.getId()))

            .map(WorkflowMapper::mapFromWorkflowVersionToWorkflowVersionResponseDto);
  }

  @Override
  public Flux<WorkflowStepResponse> getWorkflowSteps(UUID organizationId, UUID workflowVersionId) {
    return workflowVersionRepository.findByIdAndOrganizationId(workflowVersionId, organizationId)
            .switchIfEmpty(Mono.error(
                    new WorkflowNotFoundException("Workflow version not found")))
            .flatMapMany(workflowVersion ->
                    workflowStepRepository.findAllByWorkflowVersionIdOrderByPositionAsc(workflowVersion.getId()))
            .map(WorkflowMapper::mapFromWorkflowStepToWorkflowStepResponseDto);
  }

  /**
   * Verify organization ownership
   *           ↓
   * Lock DRAFT version
   *           ↓
   * Check that steps exist
   *           ↓
   * Set status = PUBLISHED
   *           ↓
   * Save workflow version
   *           ↓
   * Commit transaction
   */
  @Override
  public Mono<WorkflowVersionResponse> publishWorkflow(UUID organizationId, UUID workflowId) {
    return transactionalOperator.transactional(
            workflowRepository.findByIdAndOrganizationId(workflowId, organizationId)
                    .switchIfEmpty(Mono.error(new WorkflowNotFoundException("Workflow not found")))
                    .flatMap(workflow ->
                            workflowVersionRepository
                                    .findDraftVersionForUpdate(workflow.getId())
                                    .switchIfEmpty(Mono.error(new WorkflowInvalidStateException("Workflow has no draft version published")))
                    .flatMap(draftVersion ->
                      //step validation
                      workflowStepRepository.existsByWorkflowVersionId(draftVersion.getId())
                              .flatMap(hasSteps -> {
                                if (!hasSteps) {
                                  return Mono.error(new WorkflowInvalidStateException("Cannot publish workflow version without steps"));
                                }
                                //Actual publishing comes here: retire the currently published version (if any) and then set the draft version to PUBLISHED.
                                return retirePublishedVersion(workflow.getId())
                                        .then(Mono.defer(() -> {
                                          draftVersion.setStatus(WorkflowVersionStatus.PUBLISHED);
                                          draftVersion.setUpdatedAt(Instant.now());
                                          return workflowVersionRepository.save(draftVersion)
                                                  .map(WorkflowMapper::mapFromWorkflowVersionToWorkflowVersionResponseDto);

                                        }));
                              })
                    ))
    );
  }

  /**
   *  Once a workflow version is PUBLISHED, it cannot be modified directly.
   * Instead, FlowGuard will create a new DRAFT version that can be edited independently.
   * For example:
   * Employee Onboarding
   *     |
   *     +-- Version 1 (PUBLISHED)
   *     |       +-- Review Employee Documents
   *     |       +-- Create Employee Account
   *     |
   *     +-- Version 2 (DRAFT)
   *             +-- Review Employee Documents
   *             +-- Create Employee Account
   *             +-- New steps can be added
   * Eventually, the createDraftVersion() will:
   * Verify workflow ownership using the authenticated organization's ID.
   * Prevent creating a second draft if one already exists.
   * Determine the next version number.
   * Copy the previous version's steps into the new draft.
   * Save everything in a transaction with appropriate locking.
   */
  @Override
  public Mono<WorkflowVersionResponse> createDraftVersion(UUID organizationId, UUID workflowId) {
    return transactionalOperator.transactional(
            workflowRepository.findByIdAndOrganizationIdForUpdate(workflowId, organizationId)
                    .switchIfEmpty(Mono.error(new WorkflowNotFoundException("Workflow not found")))
                    .flatMap(workflow ->
                      workflowVersionRepository.findByWorkflowIdAndStatus(workflow.getId(), WorkflowVersionStatus.DRAFT)
                              .flatMap(existingDraft -> Mono.<WorkflowVersionResponse>error(new WorkflowInvalidStateException("Workflow already has a draft version")))
                              .switchIfEmpty(Mono.defer(() ->
                                      //Mono.<WorkflowVersionResponse>error(new UnsupportedOperationException("Draft version creation not implemented yet"))
                                      workflowVersionRepository.findFirstByWorkflowIdOrderByVersionDesc(workflow.getId())
                                              .switchIfEmpty(Mono.error(new WorkflowInvalidStateException("Workflow has no existing version")))
                                              .flatMap(latestVersion -> {
                                                int nextVersion = latestVersion.getVersion() + 1;
                                                Instant now = Instant.now();
                                                WorkflowVersion draftVersion = WorkflowVersion.builder()
                                                        .workflowId(workflow.getId())
                                                        .version(nextVersion)
                                                        .status(WorkflowVersionStatus.DRAFT)
                                                        .createdAt(now)
                                                        .updatedAt(now)
                                                        .build();
                                                return workflowVersionRepository.save(draftVersion)
                                                        //copy the steps from the latest version to the new draft version
                                                        .flatMap(saveDraft -> copyWorkflowSteps(latestVersion.getId(), saveDraft.getId()).thenReturn(saveDraft))
                                                        .map(WorkflowMapper::mapFromWorkflowVersionToWorkflowVersionResponseDto);
                                              })
                                      )
                              )

                    )
    );
  }

  //helper method to copy the workflow steps when creating version 2. We want to copy the steps from version 1 to version 2.
  // This is a common pattern in workflow management systems where each version of a workflow
  // can have its own set of steps, but we want to start with the same steps as the previous version.

  private Mono<Void> copyWorkflowSteps(UUID sourceVersionId, UUID targetVersionId){
    return workflowStepRepository.findAllByWorkflowVersionIdOrderByPositionAsc(sourceVersionId)
            .map(sourceStep -> {
              Instant now = Instant.now();
              return WorkflowStep.builder()
                      .workflowVersionId(targetVersionId)
                      .name(sourceStep.getName())
                      .type(sourceStep.getType())
                      .position(sourceStep.getPosition())
                      .createdAt(now)
                      .updatedAt(now)
                      .build();
            })
            .collectList()
            .flatMap(steps -> {
              if (steps.isEmpty()) {
                return Mono.empty();
              }
              //else
              return workflowStepRepository.saveAll(steps).then();
            });
  }

  //helper method to retire the published version.
  //What does this method do?
  //When version 2 is ready to be published, it will:
  //1. Find the currently PUBLISHED version.
  //2. Lock that version using PostgreSQL FOR UPDATE.
  //3. Change its status to RETIRED.
  //4. Save the updated version.
  //If no published version exists, the helper completes without making changes.
  // This is important because the first workflow publication has no previous version to retire.
  private Mono<Void> retirePublishedVersion(UUID workflowId){
    return workflowVersionRepository.findPublishedVersionForUpdate(workflowId)
            .flatMap(publishedVersion -> {
              publishedVersion.setStatus(WorkflowVersionStatus.RETIRED);
              publishedVersion.setUpdatedAt(Instant.now());
              return workflowVersionRepository.save(publishedVersion);
            }).then();
  }

}
