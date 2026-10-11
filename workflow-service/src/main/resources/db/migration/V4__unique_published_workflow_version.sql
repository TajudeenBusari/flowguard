CREATE UNIQUE INDEX uq_workflow_one_published_version
ON workflow_versions (workflow_id)
WHERE status = 'PUBLISHED';