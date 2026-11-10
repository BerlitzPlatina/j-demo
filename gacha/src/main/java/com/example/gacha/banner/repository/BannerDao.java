package com.example.gacha.banner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.gacha.banner.entity.Banner;

/**
 * Banner Dao. Searches go through {@link BannerSpecifications}; what is left here is the
 * uniqueness check the service runs before it inserts.
 */
@Repository
public interface BannerDao extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Banner> findByCode(String code);
}
