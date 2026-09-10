package com.example.erp.department.repository;

import com.example.erp.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Department Dao. Derived queries only; anything that needs a projection or a bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface DepartmentDao extends JpaRepository<Department, Long> {

    Page<Department> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    /** Rejects a second row with the same name on update, ignoring the row being updated. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByDepartmentCodeIgnoreCase(String departmentCode);

    /** Same guard as above for the second unique column, the business key. */
    boolean existsByDepartmentCodeIgnoreCaseAndIdNot(String departmentCode, Long id);
}
