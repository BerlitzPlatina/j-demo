-- Baseline: the user table.
--
-- A versioned migration runs exactly once, in version order, and is then recorded in
-- flyway_schema_history with a checksum. Once it has run anywhere, never edit this file:
-- change the schema by adding V2, V3, ... instead.
CREATE TABLE orm_user (
    id               INT         NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    name             VARCHAR(32) NOT NULL UNIQUE COMMENT 'user name',
    password         VARCHAR(32) NOT NULL COMMENT 'hashed password',
    salt             VARCHAR(32) NOT NULL COMMENT 'password salt',
    email            VARCHAR(32) NOT NULL UNIQUE COMMENT 'email',
    phone_number     VARCHAR(15) NOT NULL UNIQUE COMMENT 'phone number',
    status           INT         NOT NULL DEFAULT 1 COMMENT 'status: -1 deleted, 0 disabled, 1 enabled',
    create_time      DATETIME    NOT NULL DEFAULT NOW() COMMENT 'creation time',
    last_login_time  DATETIME             DEFAULT NULL COMMENT 'last login time',
    last_update_time DATETIME    NOT NULL DEFAULT NOW() COMMENT 'last update time'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='users';
