package com.example.orm.jpa.repository;

import com.example.orm.jpa.dto.DepartmentResponse;
import com.example.orm.jpa.entity.Department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Department Dao.
 *
 * @author 76peter
 * @date Created in 2019-10-01 18:07
 */
@Repository
public interface DepartmentDao extends JpaRepository<Department, Long> {
    /**
     * Finds departments by hierarchy level.
     *
     * @param level hierarchy level
     * @return matching departments
     */
    List<Department> findDepartmentsByLevels(Integer level);

    /**
     * Loads the given departments straight into response objects: a plain
     * {@code where id in (...)} on {@code orm_department}, no entity hydration, no join.
     */
    @Query("""
            select new com.example.orm.jpa.dto.DepartmentResponse(d.id, d.name, d.levels, d.orderNo)
            from Department d
            where d.id in :ids
            order by d.orderNo, d.id
            """)
    List<DepartmentResponse> findResponsesByIdIn(@Param("ids") Collection<Long> ids);
}
