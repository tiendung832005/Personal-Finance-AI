-- Sprint 7.1: Thêm trường AI cho điểm sức khỏe (Sử dụng Procedure để tránh lỗi nếu cột đã tồn tại)
DROP PROCEDURE IF EXISTS AddAIFieldsToHealthScore;

DELIMITER //
CREATE PROCEDURE AddAIFieldsToHealthScore()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'financial_health_scores' 
        AND COLUMN_NAME = 'ai_analysis'
    ) THEN
        ALTER TABLE financial_health_scores ADD COLUMN ai_analysis TEXT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'financial_health_scores' 
        AND COLUMN_NAME = 'savings_tips'
    ) THEN
        ALTER TABLE financial_health_scores ADD COLUMN savings_tips TEXT NULL;
    END IF;
END //
DELIMITER ;

CALL AddAIFieldsToHealthScore();
DROP PROCEDURE IF EXISTS AddAIFieldsToHealthScore;
