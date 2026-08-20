package com.example.orm.jpa.service;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.blazebit.persistence.view.Sorters;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.exception.ResourceNotFoundException;
import com.example.orm.jpa.view.UserDetailView;
import com.example.orm.jpa.view.UserSummaryView;
import com.example.orm.jpa.view.UserWithDepartmentsView;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Read operations for users, projected through Blaze entity views.
 * <p>
 * The chosen view is the fetch plan: picking {@link UserWithDepartmentsView} adds the join,
 * picking {@link UserSummaryView} leaves it out. Either way it stays a single query.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    /**
     * Properties a client may sort the list by. Restricted to attributes present in
     * {@link UserSummaryView}, since Blaze can only sort by mapped attributes.
     */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "email", "status", "createTime");

    /** Blaze requires a deterministic order to paginate; fall back to the id when none is given. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public UserService(EntityManager em, CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.em = em;
        this.cbf = cbf;
        this.evm = evm;
    }

    /**
     * Returns a single user with departments.
     */
    public UserDetailView getById(Long id) {
        UserDetailView view = evm.find(em, UserDetailView.class, id);
        if (view == null) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        return view;
    }

    /**
     * Returns a page of users, optionally filtered by a case-insensitive name fragment.
     * Departments are joined in only when requested.
     */
    public Page<? extends UserSummaryView> search(String keyword, boolean includeDepartments, Pageable pageable) {
        Class<? extends UserSummaryView> viewType =
                includeDepartments ? UserWithDepartmentsView.class : UserSummaryView.class;

        return searchAs(keyword, viewType, pageable);
    }

    private <T extends UserSummaryView> Page<T> searchAs(String keyword, Class<T> viewType, Pageable pageable) {
        validateSort(pageable.getSort());

        CriteriaBuilder<User> builder = cbf.create(em, User.class);
        if (StringUtils.hasText(keyword)) {
            builder.where("name").like(false).value("%" + keyword.trim() + "%").noEscape();
        }

        EntityViewSetting<T, ?> setting = EntityViewSetting.create(
                viewType, (int) pageable.getOffset(), pageable.getPageSize());
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        for (Sort.Order order : sort) {
            setting.addAttributeSorter(order.getProperty(),
                    order.isAscending() ? Sorters.ascending() : Sorters.descending());
        }

        PagedList<T> result = (PagedList<T>) evm.applySetting(setting, builder).getResultList();
        return new PageImpl<>(result, pageable, result.getTotalSize());
    }

    private void validateSort(Sort sort) {
        sort.forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort property: " + order.getProperty() + ". Allowed: " + SORTABLE_FIELDS);
            }
        });
    }
}
