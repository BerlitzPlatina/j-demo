-- Repeatable migration: no version, runs after all versioned ones, and re-runs whenever this
-- file's checksum changes. That makes it the right home for objects that are always redefined
-- rather than altered - views, procedures, triggers.
CREATE OR REPLACE VIEW user_directory AS
SELECT u.id           AS user_id,
       u.name         AS user_name,
       u.email        AS email,
       u.status       AS status,
       d.id           AS department_id,
       d.name         AS department_name
FROM orm_user u
         LEFT JOIN orm_user_dept ud ON ud.user_id = u.id
         LEFT JOIN orm_department d ON d.id = ud.dept_id;
