CREATE TABLE workflow_steps(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_version_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    step_type VARCHAR(30) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflow_steps_workflow_version
        FOREIGN KEY (workflow_version_id)
        REFERENCES workflow_versions(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_workflow_steps_version_position
        UNIQUE (workflow_version_id, position)
);

---Workflow
--- Version 1 [DRAFT]
---  Step 1
---  Step 2
---  Step 3
--- position gives deterministic ordering, and the unique constraint prevents two steps in the same version from occupying the same position.