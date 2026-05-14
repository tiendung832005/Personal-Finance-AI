CREATE TABLE otp_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    INDEX idx_otp_email_purpose_created (email, purpose, created_at),
    INDEX idx_otp_expires_at (expires_at)
);
