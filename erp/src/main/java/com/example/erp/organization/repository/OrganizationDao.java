package com.example.erp.organization.repository;

import com.example.erp.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Organization Dao. The entity has no relations, so every read here is a single query.
 */
@Repository
public interface OrganizationDao extends JpaRepository<Organization, Long> {

    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    /** Used to reject a second row with the same name on update, ignoring the row being updated. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Organization> findFirstByDefaultOrgIsTrue();

    /**
     * Clears the default flag everywhere except {@code keepId}, in one statement.
     * <p>
     * {@code clearAutomatically} is required: this bypasses the persistence context, so any
     * organization already loaded in it would otherwise keep a stale flag.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Organization o set o.defaultOrg = false where o.defaultOrg = true and o.id <> :keepId")
    int clearDefaultExcept(@Param("keepId") Long keepId);
}
