package com.example.orm.jpa.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.example.orm.jpa.entity.Department;

/**
 * Minimal department projection embedded in {@link UserSummaryView}.
 */
@EntityView(Department.class)
public interface DepartmentSummaryView {

    @IdMapping
    Long getId();

    String getName();
}
