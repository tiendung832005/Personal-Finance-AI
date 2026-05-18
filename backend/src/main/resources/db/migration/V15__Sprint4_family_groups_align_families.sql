-- Sprint 4 T01: family_groups trong spec = bảng families (đã tạo ở V1).
-- Bổ sung updated_at để khớp mô hình FamilyGroup và audit.

ALTER TABLE families
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
