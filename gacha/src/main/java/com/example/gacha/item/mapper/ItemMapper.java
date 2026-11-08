package com.example.gacha.item.mapper;

import com.example.gacha.item.dto.ItemCreateRequest;
import com.example.gacha.item.dto.ItemPatchRequest;
import com.example.gacha.item.dto.ItemResponse;
import com.example.gacha.item.dto.ItemUpdateRequest;
import com.example.gacha.item.entity.Item;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getDescription(),
                item.getItemType(),
                item.getRarity(),
                item.getImageUrl(),
                item.getActive(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public static Item toEntity(ItemCreateRequest request) {
        return Item.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .itemType(request.itemType())
                .rarity(request.rarity())
                .imageUrl(request.imageUrl())
                .active(orTrue(request.active()))
                .build();
    }

    public static void replace(Item target, ItemUpdateRequest request) {
        target.setName(request.name());
        target.setDescription(request.description());
        target.setItemType(request.itemType());
        target.setRarity(request.rarity());
        target.setImageUrl(request.imageUrl());
        target.setActive(orTrue(request.active()));
    }

    public static void merge(Item target, ItemPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
        if (request.description() != null) {
            target.setDescription(request.description());
        }
        if (request.itemType() != null) {
            target.setItemType(request.itemType());
        }
        if (request.rarity() != null) {
            target.setRarity(request.rarity());
        }
        if (request.imageUrl() != null) {
            target.setImageUrl(request.imageUrl());
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }
    }

    private static Boolean orTrue(Boolean value) {
        return value != null ? value : Boolean.TRUE;
    }
}
