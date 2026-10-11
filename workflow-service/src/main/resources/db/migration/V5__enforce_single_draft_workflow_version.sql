CREATE UNIQUE INDEX uq_workflow_versions_single_draft
ON workflow_versions (workflow_id)
WHERE status = 'DRAFT';