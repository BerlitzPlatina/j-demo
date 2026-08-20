package com.example.orm.jpa.repository;

import com.example.orm.jpa.entity.Department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
