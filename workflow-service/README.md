#### Why use the workflowVersionId in the Query rather than the workflowId?
- A workflow can have multiple versions, and each version has its own steps.
- E.g.,:
- Employee Onboarding
  |
  +-- Version 1 (PUBLISHED)
  |       +-- Step 1: Review Documents
  |       +-- Step 2: Create Account
  |
  +-- Version 2 (DRAFT)
  +-- Step 1: Review Documents
  +-- Step 2: Create Account
  +-- Step 3: Assign Equipment

### enforce one PUBLISHED version per workflow
  Why is this important?
  Without this constraint, two versions of the same workflow could accidentally have PUBLISHED status simultaneously.
  The index allows:
  Workflow	Version	Status	Allowed
  A	1	PUBLISHED	Yes
  A	2	DRAFT	Yes
  A	3	RETIRED	Yes
  A	2	PUBLISHED while v1 is PUBLISHED	No
  Important: This is a database-level safeguard. When we implement publishing newer versions, we'll need to retire the previous published version within the same transaction.
  For now, only create the V4 migration. Do not modify any previously applied Flyway migrations.