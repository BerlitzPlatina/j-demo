INSERT INTO `orm_user`(`id`,`name`,`password`,`salt`,`email`,`phone_number`) VALUES (1, 'user_1', 'ff342e862e7c3285cdc07e56d6b8973b', '412365a109674b2dbb1981ed561a4c70', 'user1@example.com', '17300000001');
INSERT INTO `orm_user`(`id`,`name`,`password`,`salt`,`email`,`phone_number`) VALUES (2, 'user_2', '6c6bf02c8d5d3d128f34b1700cb1e32c', 'fcbdd0e8a9404a5585ea4e01d0e4d7a0', 'user2@example.com', '17300000002');

INSERT INTO `orm_department`(`id`,`name`,`superior`,`levels`,`order_no`) VALUES (1, 'Head Office', NULL, 0, 0);
INSERT INTO `orm_department`(`id`,`name`,`superior`,`levels`,`order_no`) VALUES (2, 'Engineering', 1, 1, 0);
INSERT INTO `orm_department`(`id`,`name`,`superior`,`levels`,`order_no`) VALUES (3, 'Sales', 1, 1, 1);

INSERT INTO `orm_user_dept`(`user_id`,`dept_id`) VALUES (1, 1);
INSERT INTO `orm_user_dept`(`user_id`,`dept_id`) VALUES (1, 2);
INSERT INTO `orm_user_dept`(`user_id`,`dept_id`) VALUES (2, 3);
