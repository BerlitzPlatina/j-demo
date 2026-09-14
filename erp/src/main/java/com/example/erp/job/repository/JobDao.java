package com.example.erp.job.repository;

import com.example.erp.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Job Dao. Derived queries only; anything that needs a projection or a bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface JobDao extends JpaRepository<Job, Long> {

    Page<Job> findByJobCodeContainingIgnoreCase(String jobCode, Pageable pageable);

    /** Pulls the lazy relation in with the row, so a detail response stays one query. */
    @Override
    @EntityGraph(attributePaths = {"legalEntityId"})
    Optional<Job> findById(Long id);
}
