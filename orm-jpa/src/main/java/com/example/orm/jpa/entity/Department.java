package com.example.orm.jpa.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import lombok.*;

import jakarta.persistence.*;
import java.util.Collection;

/**
 * <p>
 * Department entity
 * </p>
 *
 * @author 76peter
 * @date Created in 2019-10-01 18:07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orm_department")
@ToString(callSuper = true)
public class Department extends AbstractAuditModel {

    /**
     * Department name
     */
    @Column(name = "name", columnDefinition = "varchar(255) not null")
    private String name;

    /**
     * Parent department
     */
    @ManyToOne(cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY, optional = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "superior", referencedColumnName = "id")
    private Department superior;
    /**
     * Hierarchy level
     */
    @Column(name = "levels", columnDefinition = "int not null default 0")
    private Integer levels;
    /**
     * Sort order
     */
    @Column(name = "order_no", columnDefinition = "int not null default 0")
    private Integer orderNo;
    /**
     * Child departments. Lazy, so loading a department never drags the whole tree along.
     */
    @OneToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE }, fetch = FetchType.LAZY, mappedBy = "superior")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Collection<Department> children;

    /**
     * Users assigned to this department (inverse side)
     */
    @ManyToMany(mappedBy = "departmentList")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Collection<User> userList;

}
