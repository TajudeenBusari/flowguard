CREATE TABLE workflow_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workflow_versions_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflows (id)
        ON DELETE CASCADE,

    CONSTRAINT  unique_workflow_versions_workflow_version
        UNIQUE (workflow_id, version)
);

--- The workflow versions table is what allows FlowGuard changes workflow overtime without changing the definition that previous or
--- currently running executions depend on.
--- workflow is a permanent defínition/identity of a workflow, while workflow version is a specific implementation of that workflow at a point in time.
--- For example:
--- workflows
------------------------------------------------
--- id: 123
--- name: Purchase Approval e.g., Manager Approval -> Finance Approval → Complete
--- organization_id: Org A

--- Later the company changes the Purchase Approval to:
--- e.g., Manager Approval -> AI Risk Check → Finance Approval → Complete