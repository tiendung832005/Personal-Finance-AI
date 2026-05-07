CREATE TABLE categories (
      id        BIGINT        NOT NULL AUTO_INCREMENT,
      name      VARCHAR(100)  NOT NULL,
      icon      VARCHAR(50)       NULL,
      type      ENUM('EXPENSE','INCOME','BOTH') NOT NULL,
      color     VARCHAR(7)        NULL,
      is_system BOOLEAN       NOT NULL DEFAULT FALSE,
      user_id   BIGINT            NULL,

               PRIMARY KEY (id),
               CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
               INDEX idx_categories_user_id (user_id),
               INDEX idx_categories_type    (type)
);

INSERT INTO categories (name, icon, type, color, is_system) VALUES
       ('Ăn uống','🍜','EXPENSE','#E74C3C',TRUE), ('Đi lại','🚗','EXPENSE','#E67E22',TRUE),
       ('Giải trí','🎮','EXPENSE','#9B59B6',TRUE), ('Mua sắm','🛍','EXPENSE','#3498DB',TRUE),
       ('Sức khỏe','💊','EXPENSE','#1ABC9C',TRUE), ('Giáo dục','📚','EXPENSE','#2ECC71',TRUE),
       ('Tiện ích','💡','EXPENSE','#F39C12',TRUE), ('Nhà ở','🏠','EXPENSE','#34495E',TRUE),
       ('Du lịch','✈️','EXPENSE','#5DADE2',TRUE),  ('Cà phê','☕','EXPENSE','#A04000',TRUE),
       ('Thể thao','⚽','EXPENSE','#28B463',TRUE), ('Quà tặng','🎁','EXPENSE','#F1948A',TRUE),
       ('Lương','💰','INCOME','#27AE60',TRUE),     ('Thu nhập khác','📥','INCOME','#2980B9',TRUE),
       ('Khác','📦','BOTH','#95A5A6',TRUE);
