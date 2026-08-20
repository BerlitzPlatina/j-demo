package com.example.orm.jpa.view;

import com.blazebit.persistence.view.EntityView;
import com.example.orm.jpa.entity.User;

import java.util.Date;

/**
 * Full user projection for the single-resource endpoint.
 * Credentials (password, salt) are absent from the view, so they are never even selected.
 */
@EntityView(User.class)
public interface UserDetailView extends UserWithDepartmentsView {

    Date getLastLoginTime();

    Date getLastUpdateTime();
}
