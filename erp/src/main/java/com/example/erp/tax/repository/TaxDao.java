package com.example.erp.tax.repository;

import com.example.erp.tax.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Tax Dao.
 * <p>
 * {@link JpaSpecificationExecutor} is what keeps the search off this interface: the filters are
 * composed in {@link TaxSpecifications} and handed to {@code findAll(Specification, Pageable)},
 * so a new filter does not add a derived-query method name here. What is left are the
 * uniqueness checks, which the service runs before it writes.
 */
@Repository
public interface TaxDao extends JpaRepository<Tax, Long>, JpaSpecificationExecutor<Tax> {

    boolean existsByNameIgnoreCase(String name);

    /** Rejects a second row with the same name on update, ignoring the row being updated. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
