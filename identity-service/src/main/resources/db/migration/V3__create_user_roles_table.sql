CREATE TABLE user_role(
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_role(user_id);

---the composite primary key (user_id, role) ensures that each user
-- can have a unique combination of roles,
-- preventing duplicate role assignments for the same user.
-- meaning that no single user can have the same role assigned more than once
-- ON DELETE CASCADE ensures once a user is deleted, all their associated roles are automatically
-- removed from the user_role table, maintaining referential integrity.