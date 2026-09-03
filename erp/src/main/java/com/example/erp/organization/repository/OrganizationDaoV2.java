package com.example.erp.organization.repository;

import com.example.erp.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Organization Dao, entity-graph flavour.
 * <p>
 * Same reads as {@link OrganizationDao}, but the graph is attached to purpose-named methods
 * instead of to the inherited {@code findById}. Overriding the inherited method attaches the
 * graph to <em>every</em> caller, so the update, patch and delete paths - none of which read the
 * addresses - end up joining them too. Here a caller that only needs the row keeps using the
 * plain inherited {@code findById}.
 * <p>
 * The graphs are declared with {@code attributePaths} rather than a {@code @NamedEntityGraph} on
 * the entity, so the whole thing lives in this one file.
 * <p>
 * Note on the paged read: a fetch join plus {@code LIMIT} used to mean Hibernate loading the
 * whole result set and paging it in memory, because the limit counted joined rows rather than
 * organizations. Hibernate 6+ rewrites it instead - the paged query over {@code organizations}
 * becomes a derived table and the addresses are joined onto that - so the limit is applied to
 * organizations and {@link #findByNameContainingIgnoreCase} below is a single correct query.
 * Verified against Hibernate 7.4 / MySQL 8.4: page size 2 over 3 organizations returns 2
 * organizations with all of their addresses, and no HHH90003004 warning.
 */
@Repository
public interface OrganizationDaoV2 extends JpaRepository<Organization, Long> {

    /** One page of organizations with their addresses, in one query. */
    @EntityGraph(attributePaths = "addresses")
    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * One organization with its addresses.
     * <p>
     * Named {@code findWithAddressesById} rather than {@code findById}: Spring Data ignores the
     * words between {@code find} and {@code By}, so this is still a lookup on the id - it just
     * does not shadow the inherited method.
     */
    @EntityGraph(attributePaths = "addresses")
    Optional<Organization> findWithAddressesById(Long id);

    /**
     * The default organization with its addresses. Spelled as a {@code @Query} because
     * {@code findFirstWithAddressesByDefaultOrgIsTrue} would read as if the row were picked
     * arbitrarily, when in fact the service keeps the flag on exactly one row.
     */
    @EntityGraph(attributePaths = "addresses")
    @Query("select o from Organization o where o.defaultOrg = true")
    Optional<Organization> findDefaultWithAddresses();
}
