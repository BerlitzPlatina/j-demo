package com.example.gacha.gachaconfig.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.gacha.gachaconfig.entity.BannerPoolItem;

@Repository
public interface BannerPoolItemDao extends JpaRepository<BannerPoolItem, Long> {

    /** The pool with its items, in one query, highest rarity and featured first. */
    @Query("""
            select p from BannerPoolItem p join fetch p.item i
            where p.bannerId = :bannerId
            order by i.rarity desc, p.featured desc, i.code
            """)
    List<BannerPoolItem> findWithItemByBannerId(@Param("bannerId") Long bannerId);

    /** See {@link BannerRarityRateDao#deleteAllByBannerId} for why this is a bulk delete. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BannerPoolItem p where p.bannerId = :bannerId")
    void deleteAllByBannerId(@Param("bannerId") Long bannerId);

    boolean existsByItemId(Long itemId);

    /**
     * Whether the item sits in the pool of a banner that is published and has started -
     * players have pulled it under its current rarity, so that rarity must not change.
     */
    @Query("""
            select count(p) > 0 from BannerPoolItem p, Banner b
            where b.id = p.bannerId and p.item.id = :itemId
              and b.status = com.example.gacha.banner.entity.BannerStatus.PUBLISHED
              and b.startTime <= :now
            """)
    boolean existsInStartedBanner(@Param("itemId") Long itemId, @Param("now") LocalDateTime now);
}
