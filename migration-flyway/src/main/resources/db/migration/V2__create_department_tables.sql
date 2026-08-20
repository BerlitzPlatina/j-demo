-- Departments and the user/department join table.
CREATE TABLE orm_department (
    id               INT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    name             VARCHAR(255) NOT NULL COMMENT 'department name',
    superior         INT                   DEFAULT NULL COMMENT 'parent department id',
    levels           INT          NOT NULL DEFAULT 0 COMMENT 'hierarchy level',
    order_no         INT          NOT NULL DEFAULT 0 COMMENT 'sort order',
    create_time      DATETIME     NOT NULL DEFAULT NOW() COMMENT 'creation time',
    last_update_time DATETIME     NOT NULL DEFAULT NOW() COMMENT 'last update time',
    CONSTRAINT fk_department_superior FOREIGN KEY (superior) REFERENCES orm_department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='departments';

CREATE TABLE orm_user_dept (
    id               INT      NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    user_id          INT      NOT NULL COMMENT 'user id',
    dept_id          INT      NOT NULL COMMENT 'department id',
    create_time      DATETIME NOT NULL DEFAULT NOW() COMMENT 'creation time',
    last_update_time DATETIME NOT NULL DEFAULT NOW() COMMENT 'last update time',
    CONSTRAINT uk_user_dept UNIQUE (user_id, dept_id),
    CONSTRAINT fk_user_dept_user FOREIGN KEY (user_id) REFERENCES orm_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_dept_dept FOREIGN KEY (dept_id) REFERENCES orm_department (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='user to department assignment';
