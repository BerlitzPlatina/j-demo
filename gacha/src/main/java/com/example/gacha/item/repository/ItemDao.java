package com.example.gacha.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.gacha.item.entity.Item;

@Repository
public interface ItemDao extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    boolean existsByCodeIgnoreCase(String code);
}
