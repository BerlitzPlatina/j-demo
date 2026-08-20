package com.example.orm.jpa.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.Mapping;
import com.example.orm.jpa.entity.User;

import java.util.Set;

/**
 * {@link UserSummaryView} plus departments. Selecting this view is what makes the join happen,
 * so a client that does not ask for departments never pays for them.
 */
@EntityView(User.class)
public interface UserWithDepartmentsView extends UserSummaryView {

    @Mapping("departmentList")
    Set<DepartmentSummaryView> getDepartments();
}
