package com.example.erp.designation.repository;

import com.example.erp.designation.entity.Designation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Designation Dao. Derived queries only; anything that needs a projection or a bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface DesignationDao extends JpaRepository<Designation, Long> {

    Page<Designation> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    /** Rejects a second row with the same name on update, ignoring the row being updated. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
