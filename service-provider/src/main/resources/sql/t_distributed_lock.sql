CREATE TABLE IF NOT EXISTS t_distributed_lock (
    lock_key VARCHAR(191) NOT NULL COMMENT '锁 key',
    lock_owner VARCHAR(120) NOT NULL COMMENT '持有者标识',
    lock_count INT NOT NULL COMMENT '重入次数',
    expire_at DATETIME(3) NOT NULL COMMENT '租约到期时间',
    PRIMARY KEY (lock_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = 'MySQL 分布式锁租约表';
