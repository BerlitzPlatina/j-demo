package com.example.erp.organization.repository;

import com.example.erp.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Organization Dao.
 * <p>
 * {@code findById} pulls the addresses in with an entity graph, so a detail response is one
 * query. The paged read deliberately has no graph: it leaves the collection lazy so the
 * {@code @Fetch(FetchMode.SUBSELECT)} on {@link Organization#getAddresses()} is what loads it -
 * one extra statement for the whole page instead of one per row.
 * <p>
 * That is one statement more than an {@code @EntityGraph} here would cost, and the subselect
 * ignores {@code limit}/{@code offset}, so it loads the addresses of every organization matching
 * the keyword rather than only the page. {@code OrganizationAddressFetchTest} pins both numbers
 * down; putting {@code @EntityGraph(attributePaths = "addresses")} back on the method below is
 * the one-line fix.
 */
@Repository
public interface OrganizationDao extends JpaRepository<Organization, Long> {

    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "addresses")
    Optional<Organization> findById(Long id);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Used to reject a second row with the same name on update, ignoring the row
     * being updated.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @EntityGraph(attributePaths = "addresses")
    Optional<Organization> findFirstByDefaultOrgIsTrue();

    /**
     * Clears the default flag everywhere except {@code keepId}, in one statement.
     * <p>
     * {@code clearAutomatically} is required: this bypasses the persistence
     * context, so any
     * organization already loaded in it would otherwise keep a stale flag.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Organization o set o.defaultOrg = false where o.defaultOrg = true and o.id <> :keepId")
    int clearDefaultExcept(@Param("keepId") Long keepId);
}
