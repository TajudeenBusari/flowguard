---flyway migration should remain immutable once applied, create another migration to remove the column instead of modifying this one---
---Then remove token_version from the User entity and remove the corresponding field from the UserDto if included---
ALTER TABLE users
    DROP COLUMN token_version;