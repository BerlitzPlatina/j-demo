package com.example.gacha.gachaconfig.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.gacha.gachaconfig.entity.BannerRarityRate;

@Repository
public interface BannerRarityRateDao extends JpaRepository<BannerRarityRate, Long> {

    List<BannerRarityRate> findByBannerIdOrderByRarityDesc(Long bannerId);

    /**
     * A bulk delete, run immediately: replacing a table is delete-then-insert, and Hibernate
     * would otherwise flush the inserts first and trip the (banner, rarity) unique key.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BannerRarityRate r where r.bannerId = :bannerId")
    void deleteAllByBannerId(@Param("bannerId") Long bannerId);
}
