CREATE TABLE family_members (
                                id         BIGINT        NOT NULL AUTO_INCREMENT,
                                family_id  BIGINT        NOT NULL,
                                user_id    BIGINT        NOT NULL,
                                role       ENUM('ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER',
                                nickname   VARCHAR(50)       NULL,
                                joined_at  DATETIME      NOT NULL DEFAULT NOW(),
                                invited_by BIGINT            NULL,
                                PRIMARY KEY (id),
                                UNIQUE KEY uq_family_member (family_id, user_id),
                                CONSTRAINT fk_fm_family     FOREIGN KEY (family_id)  REFERENCES families(id) ON DELETE CASCADE,
                                CONSTRAINT fk_fm_user       FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
                                CONSTRAINT fk_fm_invited_by FOREIGN KEY (invited_by) REFERENCES users(id)    ON DELETE SET NULL,
                                INDEX idx_family_members_family (family_id),
                                INDEX idx_family_members_user   (user_id)
);
