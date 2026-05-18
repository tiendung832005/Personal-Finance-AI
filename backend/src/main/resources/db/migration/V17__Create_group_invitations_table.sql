-- Sprint 4 T03: lời mời tham gia nhóm (group = family trong DB hiện tại).

CREATE TABLE group_invitations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    group_id   BIGINT       NOT NULL,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(64)  NOT NULL,
    status     ENUM('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME     NOT NULL,
    created_by BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_invitations_token (token),
    INDEX idx_group_invitations_group (group_id),
    INDEX idx_group_invitations_group_email_status (group_id, email, status),
    CONSTRAINT fk_group_invitations_group FOREIGN KEY (group_id) REFERENCES families (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_invitations_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);
