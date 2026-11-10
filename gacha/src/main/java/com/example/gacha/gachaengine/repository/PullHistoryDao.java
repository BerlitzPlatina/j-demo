package com.example.gacha.gachaengine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.gacha.gachaengine.entity.PullHistory;

@Repository
public interface PullHistoryDao extends JpaRepository<PullHistory, Long> {

    @Query("SELECT p FROM PullHistory p WHERE p.userCode = ?1 ORDER BY p.createdAt DESC")
    List<PullHistory> findByUserCodeOrderByNewest(String userCode);

    @Query("SELECT p FROM PullHistory p WHERE p.userCode = ?1 AND p.bannerId = ?2 ORDER BY p.createdAt DESC")
    List<PullHistory> findByUserCodeAndBannerIdOrderByNewest(String userCode, Long bannerId);

    @Query("SELECT p FROM PullHistory p WHERE p.userCode = ?1 AND p.bannerId = ?2 ORDER BY p.createdAt DESC LIMIT ?3")
    List<PullHistory> findLatestByUserCodeAndBannerId(String userCode, Long bannerId, int limit);

    @Query("SELECT p FROM PullHistory p WHERE p.userCode = ?1 AND p.rarity >= ?2 ORDER BY p.createdAt DESC")
    List<PullHistory> findByUserCodeAndRarityGreaterOrEqual(String userCode, Integer rarity);

    @Query("SELECT COUNT(p) FROM PullHistory p WHERE p.userCode = ?1 AND p.bannerId = ?2")
    long countByUserCodeAndBannerId(String userCode, Long bannerId);

    @Query("SELECT COUNT(p) FROM PullHistory p WHERE p.userCode = ?1 AND p.bannerId = ?2 AND p.isFeatured = true")
    long countFeaturedByUserCodeAndBannerId(String userCode, Long bannerId);
}
