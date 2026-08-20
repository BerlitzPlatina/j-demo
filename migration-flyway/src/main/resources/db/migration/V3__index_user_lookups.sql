-- Schema change on an existing table: this is what a later version looks like.
-- The paged listing filters on name and orders by create_time, so both get an index.
CREATE INDEX idx_user_name ON orm_user (name);
CREATE INDEX idx_user_create_time ON orm_user (create_time);
