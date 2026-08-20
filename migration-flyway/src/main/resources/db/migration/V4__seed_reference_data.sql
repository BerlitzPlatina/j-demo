-- Reference data, seeded idempotently: re-running this statement on a database that already has
-- the rows changes nothing, which keeps the migration safe to replay on a restored dump.
--
-- ${default_department} is a Flyway placeholder, resolved from spring.flyway.placeholders.
INSERT INTO orm_department (id, name, superior, levels, order_no)
VALUES (1, '${default_department}', NULL, 0, 0) AS new
ON DUPLICATE KEY UPDATE name = new.name;

INSERT INTO orm_department (id, name, superior, levels, order_no)
VALUES (2, 'Engineering', 1, 1, 0),
       (3, 'Sales', 1, 1, 1) AS new
ON DUPLICATE KEY UPDATE name = new.name;

INSERT INTO orm_user (id, name, password, salt, email, phone_number)
VALUES (1, 'user_1', 'ff342e862e7c3285cdc07e56d6b8973b', '412365a109674b2dbb1981ed561a4c70',
        'user1@example.com', '17300000001'),
       (2, 'user_2', '6c6bf02c8d5d3d128f34b1700cb1e32c', 'fcbdd0e8a9404a5585ea4e01d0e4d7a0',
        'user2@example.com', '17300000002') AS new
ON DUPLICATE KEY UPDATE name = new.name;
