package com.example.orm.jpa.service;

import com.example.common.web.dto.PageResponse;
import com.example.orm.jpa.dto.UserCreateRequest;
import com.example.orm.jpa.dto.UserInclude;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.dto.UserUpdateRequest;
import com.example.orm.jpa.entity.Department;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the full CRUD round trip and the paging contract.
 */
@SpringBootTest
@Transactional
class UserServiceCrudTest {

    /** No relation requested: the lazy collections must stay untouched. */
    private static final Set<UserInclude> NONE = Set.of();
    private static final Set<UserInclude> DEPARTMENTS = Set.of(UserInclude.DEPARTMENTS);

    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private DepartmentDao departmentDao;

    private Long itId;
    private Long salesId;

    @BeforeEach
    void setUp() {
        itId = departmentDao.save(Department.builder().name("crud_IT").levels(0).orderNo(0).build()).getId();
        salesId = departmentDao.save(Department.builder().name("crud_Sales").levels(0).orderNo(1).build()).getId();
    }

    private UserCreateRequest request(String suffix, List<Long> departmentIds) {
        return new UserCreateRequest(
                "crud_" + suffix, "secret123", "crud" + suffix + "@example.com",
                "1730200" + suffix, 1, departmentIds);
    }

    @Test
    void createReadUpdateDelete() {
        UserResponse created = userService.create(request("1", List.of(itId, salesId)));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("crud_1");
        assertThat(created.departments()).extracting("name")
                .containsExactlyInAnyOrder("crud_IT", "crud_Sales");

        UserResponse read = userService.getById(created.id(), DEPARTMENTS);
        assertThat(read.email()).isEqualTo("crud1@example.com");
        assertThat(read.departments()).hasSize(2);

        UserResponse updated = userService.update(created.id(), new UserUpdateRequest(
                "crud_1_renamed", "renamed@example.com", "17302009999", 0, List.of(salesId)));
        assertThat(updated.name()).isEqualTo("crud_1_renamed");
        assertThat(updated.status()).isZero();
        assertThat(updated.departments()).extracting("name").containsExactly("crud_Sales");

        userService.delete(created.id());
        assertThat(userDao.findById(created.id())).isEmpty();
        // The departments outlive the user; only the join rows are gone.
        assertThat(departmentDao.findById(salesId)).isPresent();
    }

    @Test
    void responseNeverCarriesCredentials() {
        UserResponse created = userService.create(request("2", List.of()));

        assertThat(created.departments()).isEmpty();
        // UserResponse has no password/salt component at all, so the record's fields are the contract.
        assertThat(UserResponse.class.getRecordComponents())
                .extracting("name")
                .doesNotContain("password", "salt");
    }

    @Test
    void pagingReturnsMetadataAndRespectsPageSize() {
        for (int i = 0; i < 7; i++) {
            userService.create(request("p" + i, List.of(itId)));
        }

        PageResponse<UserResponse> firstPage = userService.search(
                "crud_p", NONE, PageRequest.of(0, 3, Sort.by("id")));

        assertThat(firstPage.content()).hasSize(3);
        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(3);
        assertThat(firstPage.totalElements()).isEqualTo(7);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();
        // Not requested, so the field stays out of the payload.
        assertThat(firstPage.content().get(0).departments()).isNull();

        PageResponse<UserResponse> lastPage = userService.search(
                "crud_p", DEPARTMENTS, PageRequest.of(2, 3, Sort.by("id")));
        assertThat(lastPage.content()).hasSize(1);
        assertThat(lastPage.last()).isTrue();
        assertThat(lastPage.content().get(0).departments()).extracting("name").containsExactly("crud_IT");
    }

    @Test
    void includeDecidesWhatIsLoaded() {
        UserResponse created = userService.create(request("4", List.of(itId, salesId)));

        // Not requested: the field is absent from the payload, not an empty list.
        assertThat(userService.getById(created.id(), NONE).departments()).isNull();
        assertThat(userService.getById(created.id(), DEPARTMENTS).departments()).hasSize(2);

        assertThat(UserInclude.parse(null)).isEmpty();
        assertThat(UserInclude.parse(List.of("DEPARTMENTS", " departments ")))
                .containsExactly(UserInclude.DEPARTMENTS);
        assertThatThrownBy(() -> UserInclude.parse(List.of("department")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Allowed: departments");
    }

    @Test
    void unknownIdsAndSortsAreRejected() {
        assertThatThrownBy(() -> userService.getById(-1L, NONE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> userService.delete(-1L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> userService.create(request("3", List.of(-1L))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> userService.search(null, NONE, PageRequest.of(0, 10, Sort.by("password"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
