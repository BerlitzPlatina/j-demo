package com.example.orm.jpa.repository;

import com.example.orm.jpa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Dao.
 * <p>
 * Two fetch strategies live here, and which one is correct depends on paging:
 * <ul>
 *   <li>Unpaged: fetch join / {@code @EntityGraph}, one query, see {@link #findAllWithDepartments()}.</li>
 *   <li>Paged: users only. A fetch join plus {@link Pageable} would force Hibernate to paginate
 *   in memory (HHH90003004), so the departments of the page are loaded separately through
 *   {@code UserDepartmentDao} and {@code DepartmentDao} and matched in memory instead.</li>
 * </ul>
 */
@Repository
public interface UserDao extends JpaRepository<User, Long> {

    /**
     * One page of users, collections untouched.
     */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Every user with its departments, in one query.
     * <p>
     * A fetch join is exactly the right tool here: without {@code limit} there is nothing for
     * Hibernate to paginate, so the duplicate rows the join produces are simply collapsed in
     * memory. {@code @EntityGraph} says which relation to fetch without hand-writing the join.
     */
    @EntityGraph(attributePaths = "departmentList")
    @Query("select u from User u")
    List<User> findAllWithDepartments();

    /**
     * Single user with departments in one query.
     */
    @Query("select u from User u left join fetch u.departmentList where u.id = :id")
    Optional<User> findDetailById(@Param("id") Long id);
}
