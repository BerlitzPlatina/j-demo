package com.example.orm.jpa.entity;

import com.example.orm.jpa.entity.base.AbstractAuditModel;
import lombok.*;

import jakarta.persistence.*;
import java.util.Collection;

/**
 * <p>
 * 部门实体类
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
     * 部门名
     */
    @Column(name = "name", columnDefinition = "varchar(255) not null")
    private String name;

    /**
     * 上级部门id
     */
    @ManyToOne(cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY, optional = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "superior", referencedColumnName = "id")
    private Department superior;
    /**
     * 所属层级
     */
    @Column(name = "levels", columnDefinition = "int not null default 0")
    private Integer levels;
    /**
     * 排序
     */
    @Column(name = "order_no", columnDefinition = "int not null default 0")
    private Integer orderNo;
    /**
     * 子部门集合
     */
    @OneToMany(cascade = { CascadeType.REFRESH, CascadeType.REMOVE }, fetch = FetchType.LAZY, mappedBy = "superior")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Collection<Department> children;

    /**
     * 部门下用户集合
     */
    @ManyToMany(mappedBy = "departmentList")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Collection<User> userList;

}
