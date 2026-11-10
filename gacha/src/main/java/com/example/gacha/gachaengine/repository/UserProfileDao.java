package com.example.gacha.gachaengine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.gacha.gachaengine.entity.UserProfile;

@Repository
public interface UserProfileDao extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserCodeAndBannerId(String userCode, Long bannerId);

    Optional<UserProfile> findByUserCodeAndBannerCode(String userCode, String bannerCode);

    List<UserProfile> findByUserCode(String userCode);

    List<UserProfile> findByBannerId(Long bannerId);

    @Query("SELECT p FROM UserProfile p WHERE p.userCode = ?1 AND p.active = true")
    List<UserProfile> findActiveByUserCode(String userCode);

    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.userCode = ?1 AND p.bannerId = ?2")
    long countByUserCodeAndBannerId(String userCode, Long bannerId);
}
