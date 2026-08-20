package com.example.orm.jpa.service;

import com.example.orm.jpa.dto.PageResponse;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the paged endpoint against N+1: the query count must stay constant when the page
 * size grows, whether or not the departments are included.
 */
@SpringBootTest
@Transactional
class UserPagingQueryCountTest {

    /** Page query + count query. */
    private static final long QUERIES_WITHOUT_DEPARTMENTS = 2;
    /** Page query + count query + one collection query for the whole page. */
    private static final long QUERIES_WITH_DEPARTMENTS = 3;

    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private EntityManager em;

    private void seed(int users) {
        Department a = departmentDao.save(Department.builder().name("probe_IT").levels(0).orderNo(0).build());
        Department b = departmentDao.save(Department.builder().name("probe_Sales").levels(0).orderNo(1).build());
        for (int i = 0; i < users; i++) {
            userDao.save(User.builder()
                    .name("probe_" + i).password("p").salt("s")
                    .email("probe" + i + "@example.com").phoneNumber("1730100" + i)
                    .status(1).departmentList(List.of(a, b)).build());
        }
        em.flush();
        em.clear();
    }

    private long countQueries(int pageSize, boolean includeDepartments) {
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        PageResponse<UserResponse> page = userService.search(
                "probe", includeDepartments, PageRequest.of(0, pageSize, Sort.by("id")));

        assertThat(page.content()).hasSize(pageSize);
        assertThat(page.totalElements()).isEqualTo(20);
        if (includeDepartments) {
            assertThat(page.content().get(0).departments())
                    .extracting("name").containsExactlyInAnyOrder("probe_IT", "probe_Sales");
        }

        long count = stats.getPrepareStatementCount();
        em.clear();
        return count;
    }

    @Test
    void queryCountIsFlatWithDepartments() {
        seed(20);

        assertThat(countQueries(2, true)).isEqualTo(QUERIES_WITH_DEPARTMENTS);
        assertThat(countQueries(20, true)).isEqualTo(QUERIES_WITH_DEPARTMENTS);
    }

    @Test
    void queryCountIsFlatWithoutDepartments() {
        seed(20);

        assertThat(countQueries(2, false)).isEqualTo(QUERIES_WITHOUT_DEPARTMENTS);
        assertThat(countQueries(20, false)).isEqualTo(QUERIES_WITHOUT_DEPARTMENTS);
    }

    @Test
    void detailIsASingleQuery() {
        seed(20);
        Long id = userDao.findByNameContainingIgnoreCase("probe", PageRequest.of(0, 1, Sort.by("id")))
                .getContent().get(0).getId();
        em.clear();

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        UserResponse user = userService.getById(id);

        assertThat(user.departments()).hasSize(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }
}
