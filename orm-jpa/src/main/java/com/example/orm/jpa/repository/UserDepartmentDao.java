package com.example.orm.jpa.repository;

import com.example.orm.jpa.dto.UserDepartmentLink;
import com.example.orm.jpa.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Read access to the user/department join table.
 */
@Repository
public interface UserDepartmentDao extends JpaRepository<UserDepartment, Long> {

    /**
     * Links belonging to the given users. Hits the join table only: no join, no user column,
     * no department column.
     */
    @Query("""
            select new com.example.orm.jpa.dto.UserDepartmentLink(ud.userId, ud.deptId)
            from UserDepartment ud
            where ud.userId in :userIds
            """)
    List<UserDepartmentLink> findLinksByUserIdIn(@Param("userIds") Collection<Long> userIds);
}
