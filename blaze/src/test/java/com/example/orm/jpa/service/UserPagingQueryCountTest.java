package com.example.orm.jpa.service;

import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import com.example.orm.jpa.view.UserSummaryView;
import com.example.orm.jpa.view.UserWithDepartmentsView;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the paged endpoint against N+1: the query count must not grow with page size,
 * whichever view is selected.
 */
@SpringBootTest
@Transactional
class UserPagingQueryCountTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private EntityManager em;

    private void seed(int users) {
        Department a = departmentDao.save(Department.builder().name("IT").levels(0).orderNo(0).build());
        Department b = departmentDao.save(Department.builder().name("Sales").levels(0).orderNo(1).build());
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

        Page<? extends UserSummaryView> page = userService.search(
                "probe", includeDepartments, PageRequest.of(0, pageSize, Sort.by("id")));

        assertThat(page.getContent()).hasSize(pageSize);
        assertThat(page.getTotalElements()).isEqualTo(20);
        if (includeDepartments) {
            UserWithDepartmentsView view = (UserWithDepartmentsView) page.getContent().get(0);
            assertThat(view.getDepartments()).extracting("name").containsExactlyInAnyOrder("IT", "Sales");
        }

        long count = stats.getPrepareStatementCount();
        em.clear();
        return count;
    }

    @Test
    void oneQueryWithDepartmentsRegardlessOfPageSize() {
        seed(20);

        assertThat(countQueries(2, true)).isEqualTo(1);
        assertThat(countQueries(20, true)).isEqualTo(1);
    }

    @Test
    void oneQueryWithoutDepartmentsRegardlessOfPageSize() {
        seed(20);

        assertThat(countQueries(2, false)).isEqualTo(1);
        assertThat(countQueries(20, false)).isEqualTo(1);
    }
}
