CREATE TABLE IF NOT EXISTS `ai_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Khoá chính',
  `order_code` VARCHAR(32) NOT NULL UNIQUE COMMENT 'Mã đơn hàng',
  `customer_name` VARCHAR(64) NOT NULL COMMENT 'Tên khách hàng',
  `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW, PACKING, SHIPPING, DELIVERED, CANCELLED',
  `location` VARCHAR(64) DEFAULT NULL COMMENT 'Vị trí hiện tại của đơn',
  `tracking_code` VARCHAR(32) DEFAULT NULL COMMENT 'Mã vận đơn',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Giá trị đơn hàng',
  `create_time` DATETIME NOT NULL DEFAULT NOW() COMMENT 'Thời điểm tạo',
  `last_update_time` DATETIME NOT NULL DEFAULT NOW() COMMENT 'Thời điểm cập nhật gần nhất',
  KEY `idx_customer_name` (`customer_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Đơn hàng dùng cho demo Spring AI tool calling';
