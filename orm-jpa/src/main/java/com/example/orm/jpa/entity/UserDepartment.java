package com.example.orm.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The {@code orm_user_dept} join table, mapped as an entity so it can be queried on its own -
 * that is what makes "select the links, then select the departments" possible without joining
 * {@code orm_user} back in.
 * <p>
 * Read-only by design: the relationship is still owned by {@link User#getDepartmentList()}, which
 * is what writes and deletes the rows. Every column here is
 * {@code insertable = false, updatable = false} so this mapping can never write to the same table.
 */
@Entity
@Table(name = "orm_user_dept")
@Getter
@NoArgsConstructor
public class UserDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "dept_id", insertable = false, updatable = false)
    private Long deptId;
}
