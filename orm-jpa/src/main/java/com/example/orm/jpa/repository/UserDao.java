package com.example.orm.jpa.repository;

import com.example.orm.jpa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * User Dao.
 * <p>
 * Paging and collection fetching are deliberately split into two methods: a fetch join plus
 * {@link Pageable} would force Hibernate to paginate in memory (HHH90003004), so the page is
 * selected first and the collection is loaded for that page's ids only.
 */
@Repository
public interface UserDao extends JpaRepository<User, Long> {

    /**
     * One page of users, collections untouched.
     */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Loads the departments of an already selected page in a single extra query.
     */
    @Query("select u from User u left join fetch u.departmentList where u.id in :ids")
    List<User> findAllWithDepartmentsByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Single user with departments in one query.
     */
    @Query("select u from User u left join fetch u.departmentList where u.id = :id")
    Optional<User> findDetailById(@Param("id") Long id);
}
