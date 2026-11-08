package com.example.gacha.item.repository;

import org.springframework.data.jpa.domain.Specification;

import com.example.gacha.common.constant.Constants;
import com.example.gacha.common.specification.SpecBuilder;
import com.example.gacha.item.dto.ItemSearchRequest;
import com.example.gacha.item.entity.Item;

public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> from(ItemSearchRequest request) {
        return SpecBuilder.<Item>of()
                .likeAny(request.keyword(), "code", "name", "description")
                .eq("itemType", request.itemType())
                .eq("rarity", request.rarity())
                .eq("active", request.active())
                .dayRange(Constants.CREATE_TIME, request.createdFrom(), request.createdTo())
                .build();
    }
}
