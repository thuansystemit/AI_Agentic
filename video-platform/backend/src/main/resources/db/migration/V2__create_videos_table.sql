CREATE TABLE videos (
    id           BIGSERIAL       PRIMARY KEY,
    title        VARCHAR(255)    NOT NULL,
    description  TEXT            NULL,
    file_path    VARCHAR(500)    NOT NULL,
    file_name    VARCHAR(255)    NOT NULL,
    file_size    BIGINT          NULL,
    content_type VARCHAR(100)    NULL,
    user_id      BIGINT          NOT NULL,
    created_at   TIMESTAMP       NULL,
    updated_at   TIMESTAMP       NULL,
    CONSTRAINT fk_videos_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_videos_title   ON videos (title);
CREATE INDEX idx_videos_user_id ON videos (user_id);
