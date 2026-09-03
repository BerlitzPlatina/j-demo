package com.example.erp.organization;

import com.example.common.web.dto.PageResponse;
import com.example.erp.organization.dto.OrganizationResponse;
import com.example.erp.organization.entity.Address;
import com.example.erp.organization.entity.Organization;
import com.example.erp.organization.service.OrganizationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measures how the {@code addresses} collection is actually loaded, so a change to the fetch
 * strategy shows up as a number rather than as a hunch.
 * <p>
 * The counters come from Hibernate's own {@link Statistics}, which counts at the JDBC layer.
 * Counting lines in the SQL log would be misleading: {@code spring.jpa.show-sql} and
 * {@code logging.level.org.hibernate.SQL} are both on, so every statement is printed twice.
 * <p>
 * {@code @Transactional} here does double duty - it rolls the fixture back at the end, and it
 * keeps the persistence context open across the service call the way a request would.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class OrganizationAddressFetchTest {

    /** Distinctive enough that the keyword filter cannot pick up rows already in the database. */
    private static final String PREFIX = "FetchProbe";

    private static final int ORGANIZATIONS = 3;
    private static final int ADDRESSES_EACH = 2;
    private static final int PAGE_SIZE = 2;

    @Autowired
    OrganizationService organizationService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void seedAndReset() {
        for (int i = 1; i <= ORGANIZATIONS; i++) {
            Organization organization = Organization.builder()
                    .name(PREFIX + " " + i)
                    .logoUploaded(false)
                    .defaultOrg(false)
                    .taxGroupEnabled(false)
                    .build();
            entityManager.persist(organization);
            for (int a = 1; a <= ADDRESSES_EACH; a++) {
                entityManager.persist(Address.builder()
                        .organization(organization)
                        .streetAddress1("Street " + i + "-" + a)
                        .city("City " + i)
                        .country("Vietnam")
                        .build());
            }
        }
        // Flush so the queries below see the rows, clear so they are read back rather than
        // served from the persistence context - which would load no collection at all.
        entityManager.flush();
        entityManager.clear();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    /**
     * The paged read, addresses included, all the way through the mapper.
     * <p>
     * Three statements: the page of organizations, the {@code count} behind the page metadata,
     * and one subselect for the collections. Without {@code SUBSELECT} or {@code @BatchSize}
     * that last one would be a statement per organization on the page.
     */
    @Test
    void pagedReadLoadsEveryCollectionInOneExtraStatement() {
        PageResponse<OrganizationResponse> page = organizationService.search(
                PREFIX, PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(page.content()).hasSize(PAGE_SIZE);
        assertThat(page.totalElements()).isEqualTo(ORGANIZATIONS);
        assertThat(page.content()).allSatisfy(organization ->
                assertThat(organization.addresses()).hasSize(ADDRESSES_EACH));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    /**
     * The cost of {@code SUBSELECT} under pagination, stated as an assertion.
     * <p>
     * The subquery inherits the original {@code where} but not its {@code limit}, so it loads a
     * collection for every organization the keyword matches - here {@code ORGANIZATIONS}, while
     * the page only asked for {@code PAGE_SIZE}. Switching the entity to
     * {@code @BatchSize(size = 50)} drops this to {@code PAGE_SIZE}; an {@code @EntityGraph} on
     * the query drops it to {@code PAGE_SIZE} and removes a statement as well.
     */
    @Test
    void subselectLoadsCollectionsBeyondThePage() {
        organizationService.search(
                PREFIX, PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(statistics.getCollectionLoadCount())
                .as("SUBSELECT ignores limit/offset, so it reaches past the page")
                .isEqualTo(ORGANIZATIONS)
                .isGreaterThan(PAGE_SIZE);
    }

    /**
     * The single-row read has an {@code @EntityGraph}, so the collection arrives as part of the
     * same statement - one query, and nothing left to load lazily.
     */
    @Test
    void detailReadNeedsNoSecondStatement() {
        List<Long> ids = entityManager
                .createQuery("select o.id from Organization o where o.name like :p order by o.id", Long.class)
                .setParameter("p", PREFIX + "%")
                .getResultList();
        statistics.clear();

        OrganizationResponse response = organizationService.getById(ids.get(0));

        assertThat(response.addresses()).hasSize(ADDRESSES_EACH);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getCollectionFetchCount())
                .as("a graph-loaded collection is never fetched separately")
                .isZero();
    }
}
