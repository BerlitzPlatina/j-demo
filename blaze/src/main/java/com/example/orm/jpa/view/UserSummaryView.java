package com.example.orm.jpa.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.example.orm.jpa.entity.User;

import java.util.Date;

/**
 * Lightweight user projection used in paged responses. Blaze derives the SQL from these
 * getters, so only the mapped columns are selected.
 */
@EntityView(User.class)
public interface UserSummaryView {

    @IdMapping
    Long getId();

    String getName();

    String getEmail();

    String getPhoneNumber();

    Integer getStatus();

    Date getCreateTime();
}
