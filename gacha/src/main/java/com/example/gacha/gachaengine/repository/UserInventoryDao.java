package com.example.gacha.gachaengine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.gacha.gachaengine.entity.UserInventory;

@Repository
public interface UserInventoryDao extends JpaRepository<UserInventory, Long> {

    Optional<UserInventory> findByUserCodeAndItemId(String userCode, Long itemId);

    @Query("SELECT i FROM UserInventory i WHERE i.userCode = ?1 ORDER BY i.updatedAt DESC")
    List<UserInventory> findByUserCodeOrderByNewest(String userCode);

    @Query("SELECT i FROM UserInventory i WHERE i.userCode = ?1 AND i.active = true ORDER BY i.updatedAt DESC")
    List<UserInventory> findActiveByUserCodeOrderByNewest(String userCode);

    @Query("SELECT SUM(i.quantity) FROM UserInventory i WHERE i.userCode = ?1")
    Integer getTotalItemCount(String userCode);

    @Query("SELECT COUNT(i) FROM UserInventory i WHERE i.userCode = ?1 AND i.quantity > 0")
    long countItemTypesOwned(String userCode);
}
