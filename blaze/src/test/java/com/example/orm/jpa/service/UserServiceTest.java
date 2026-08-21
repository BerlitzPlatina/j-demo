package com.example.orm.jpa.service;

import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import com.example.orm.jpa.view.UserDetailView;
import com.example.orm.jpa.view.UserSummaryView;
import com.example.orm.jpa.view.UserWithDepartmentsView;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private DepartmentDao departmentDao;
    @Autowired
    private EntityManager em;

    private User newUser(String name, String suffix, List<Department> departments) {
        return userDao.save(User.builder()
                .name(name).password("secret").salt("salt")
                .email(name + "@example.com").phoneNumber("1730090" + suffix)
                .status(1).departmentList(departments).build());
    }

    @Test
    void detailFetchesDepartments() {
        Department dept = departmentDao.save(
                Department.builder().name("IT").levels(0).orderNo(0).build());
        User user = newUser("alice", "1", List.of(dept));
        em.flush();
        em.clear();

        UserDetailView detail = userService.getById(user.getId());

        assertThat(detail.getName()).isEqualTo("alice");
        assertThat(detail.getDepartments()).extracting("name").containsExactly("IT");
        assertThat(detail.getDepartments()).extracting("id").doesNotContainNull();
        assertThat(detail.getLastUpdateTime()).isNotNull();
    }

    @Test
    void missingUserRaisesNotFound() {
        assertThatThrownBy(() -> userService.getById(-1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchFiltersByNameIgnoringCase() {
        newUser("Zebra", "2", List.of());
        em.flush();
        em.clear();

        Page<? extends UserSummaryView> page = userService.search("zeb", false, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting("name").containsExactly("Zebra");
    }

    @Test
    void summaryViewHasNoDepartmentsAtAll() {
        Department dept = departmentDao.save(
                Department.builder().name("Sales").levels(0).orderNo(0).build());
        newUser("bob", "3", List.of(dept));
        em.flush();
        em.clear();

        Page<? extends UserSummaryView> page = userService.search("bob", false, PageRequest.of(0, 10));

        assertThat(page.getContent().get(0)).isNotInstanceOf(UserWithDepartmentsView.class);
    }

    @Test
    void includeSwitchesToTheDepartmentView() {
        Department dept = departmentDao.save(
                Department.builder().name("Ops").levels(0).orderNo(0).build());
        newUser("carol", "4", List.of(dept));
        em.flush();
        em.clear();

        Page<? extends UserSummaryView> page = userService.search("carol", true, PageRequest.of(0, 10));

        UserWithDepartmentsView view = (UserWithDepartmentsView) page.getContent().get(0);
        assertThat(view.getDepartments()).extracting("name").containsExactly("Ops");
    }

    @Test
    void rejectsSortOnNonWhitelistedProperty() {
        assertThatThrownBy(() -> userService.search(null, false, PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }
}
