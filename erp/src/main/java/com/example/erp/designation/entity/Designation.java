package com.example.erp.designation.entity;

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
 * The {@code designations} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so they are not
 * repeated here.
 */
@Entity
@Table(name = "designations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Designation extends AbstractAuditModel {

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
