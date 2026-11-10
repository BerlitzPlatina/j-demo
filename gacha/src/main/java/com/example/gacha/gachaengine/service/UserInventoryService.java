package com.example.gacha.gachaengine.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.gachaengine.dto.InventoryItemResponse;
import com.example.gacha.gachaengine.entity.UserInventory;
import com.example.gacha.gachaengine.repository.UserInventoryDao;
import com.example.gacha.item.repository.ItemDao;
import lombok.extern.slf4j.Slf4j;

/**
 * User inventory management service.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserInventoryService {

    private final UserInventoryDao inventoryDao;
    private final ItemDao itemDao;

    public UserInventoryService(UserInventoryDao inventoryDao, ItemDao itemDao) {
        this.inventoryDao = inventoryDao;
        this.itemDao = itemDao;
    }

    /**
     * Get user inventory (all items).
     */
    public List<InventoryItemResponse> getInventory(String userCode) {
        return inventoryDao.findByUserCodeOrderByNewest(userCode).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get user inventory (active items only).
     */
    public List<InventoryItemResponse> getActiveInventory(String userCode) {
        return inventoryDao.findActiveByUserCodeOrderByNewest(userCode).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get specific item from inventory.
     */
    public InventoryItemResponse getInventoryItem(String userCode, Long itemId) {
        return inventoryDao.findByUserCodeAndItemId(userCode, itemId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found in inventory: " + itemId));
    }

    /**
     * Get total item count in inventory.
     */
    public Integer getTotalItemCount(String userCode) {
        return inventoryDao.getTotalItemCount(userCode);
    }

    /**
     * Get number of unique item types owned.
     */
    public long getUniqueItemCount(String userCode) {
        return inventoryDao.countItemTypesOwned(userCode);
    }

    private InventoryItemResponse mapToResponse(UserInventory inventory) {
        var item = itemDao.findById(inventory.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + inventory.getItemId()));

        return InventoryItemResponse.builder()
                .id(inventory.getId())
                .itemId(inventory.getItemId())
                .itemCode(inventory.getItemCode())
                .itemName(item.getName())
                .itemType(item.getItemType().toString())
                .rarity(item.getRarity())
                .quantity(inventory.getQuantity())
                .timesObtained(inventory.getTimesObtained())
                .itemImageUrl(item.getImageUrl())
                .build();
    }
}
