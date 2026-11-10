package com.example.gacha.gachaengine.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gacha.gachaengine.dto.InventoryItemResponse;
import com.example.gacha.gachaengine.service.UserInventoryService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API for user inventory.
 * 
 * API Endpoints:
 * - GET /api/gacha/inventory/{userCode} - Get user inventory
 * - GET /api/gacha/inventory/{userCode}/active - Get active inventory
 * - GET /api/gacha/inventory/{userCode}/item/{itemId} - Get specific inventory item
 */
@Slf4j
@RestController
@RequestMapping("/api/gacha/inventory")
public class UserInventoryController {

    private final UserInventoryService inventoryService;

    public UserInventoryController(UserInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Get user inventory.
     */
    @GetMapping("/{userCode}")
    public ResponseEntity<List<InventoryItemResponse>> getInventory(@PathVariable String userCode) {
        log.info("Getting inventory for user: {}", userCode);

        List<InventoryItemResponse> inventory = inventoryService.getInventory(userCode);
        return ResponseEntity.ok(inventory);
    }

    /**
     * Get active user inventory.
     */
    @GetMapping("/{userCode}/active")
    public ResponseEntity<List<InventoryItemResponse>> getActiveInventory(@PathVariable String userCode) {
        log.info("Getting active inventory for user: {}", userCode);

        List<InventoryItemResponse> inventory = inventoryService.getActiveInventory(userCode);
        return ResponseEntity.ok(inventory);
    }

    /**
     * Get specific inventory item.
     */
    @GetMapping("/{userCode}/item/{itemId}")
    public ResponseEntity<InventoryItemResponse> getInventoryItem(@PathVariable String userCode,
            @PathVariable Long itemId) {
        log.info("Getting inventory item {} for user: {}", itemId, userCode);

        InventoryItemResponse item = inventoryService.getInventoryItem(userCode, itemId);
        return ResponseEntity.ok(item);
    }
}
