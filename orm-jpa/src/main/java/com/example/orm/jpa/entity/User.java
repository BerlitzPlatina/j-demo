package com.example.orm.jpa.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import lombok.*;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.Date;

/**
 * <p>
 * User entity
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-11-07 14:06
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "orm_user")
@ToString(callSuper = true)
public class User extends AbstractAuditModel {
    /**
     * User name
     */
    private String name;

    /**
     * Hashed password
     */
    private String password;

    /**
     * Salt used for hashing
     */
    private String salt;

    /**
     * Email
     */
    private String email;

    /**
     * Phone number
     */
    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * Status: -1 soft deleted, 0 disabled, 1 enabled
     */
    private Integer status;

    /**
     * Last login time
     */
    @Column(name = "last_login_time")
    private Date lastLoginTime;

    /**
     * Departments this user belongs to.
     * <p>
     * 1. Owning side of the many-to-many: it manages the rows of the join table.
     * 2. {@code @JoinTable#name} is the join table, {@code joinColumns} the foreign key back to
     * this owning side (User), {@code inverseJoinColumns} the foreign key to the inverse side
     * (Department).
     * 3. Fetched {@code LAZY} on purpose: an {@code EAGER} collection here means one extra select
     * per row of every user query, which is exactly the N+1 problem. Callers that need the
     * departments ask for them with a fetch join instead.
     * 4. No cascade beyond REFRESH: deleting a user must not delete its departments, it only
     * removes the join table rows.
     */
    @ManyToMany(cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinTable(name = "orm_user_dept", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "dept_id", referencedColumnName = "id"))
    private Collection<Department> departmentList;

}
