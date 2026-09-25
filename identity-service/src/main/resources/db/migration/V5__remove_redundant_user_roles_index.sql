DROP INDEX IF EXISTS idx_user_roles_user_id;
---This preserves immutable migration history while avoiding an unnecessary index that POSTGRESQL automatically
-- creates for the composite primary key (user_id, role) in the user_roles table.