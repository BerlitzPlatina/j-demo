package com.example.erp.legalentity.repository;

import com.example.erp.legalentity.entity.LegalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LegalEntity Dao. Derived queries only; anything that needs a projection or a bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface LegalEntityDao extends JpaRepository<LegalEntity, Long> {

    Page<LegalEntity> findByEntityCodeContainingIgnoreCase(String entityCode, Pageable pageable);

    boolean existsByEntityCodeIgnoreCase(String entityCode);

    /** Rejects a second row with the same entityCode on update, ignoring the row being updated. */
    boolean existsByEntityCodeIgnoreCaseAndIdNot(String entityCode, Long id);

    boolean existsByLegacyDefault(Boolean legacyDefault);

    /** Rejects a second row with the same legacyDefault on update, ignoring the row being updated. */
    boolean existsByLegacyDefaultAndIdNot(Boolean legacyDefault, Long id);
}
