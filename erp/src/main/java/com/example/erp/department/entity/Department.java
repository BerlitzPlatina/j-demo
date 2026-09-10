package com.example.erp.department.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The {@code departments} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so
 * they are not
 * repeated here.
 */
@Entity
@Table(name = "departments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Department extends AbstractAuditModel {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "department_code", nullable = false, unique = true, length = 255)
    private String departmentCode;
}
