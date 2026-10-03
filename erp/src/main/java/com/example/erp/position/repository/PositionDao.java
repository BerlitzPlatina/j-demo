package com.example.erp.position.repository;

import com.example.erp.position.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Position Dao. Derived queries only; anything that needs a projection or a
 * bulk statement is
 * spelled out with {@code @Query} rather than being pushed into a method name.
 */
@Repository
public interface PositionDao extends JpaRepository<Position, Long> {
}
