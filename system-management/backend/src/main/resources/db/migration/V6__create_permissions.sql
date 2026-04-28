CREATE TABLE category_user_permissions (
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission  permission_level NOT NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (category_id, user_id)
);

CREATE TABLE category_group_permissions (
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    permission  permission_level NOT NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (category_id, group_id)
);

CREATE INDEX idx_cup_user_id  ON category_user_permissions(user_id);
CREATE INDEX idx_cgp_group_id ON category_group_permissions(group_id);
