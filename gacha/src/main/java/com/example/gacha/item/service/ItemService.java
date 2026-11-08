package com.example.gacha.item.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.web.dto.PageResponse;
import com.example.gacha.common.page.PageableSupport;
import com.example.gacha.common.support.Entities;
import com.example.gacha.common.support.Guards;
import com.example.gacha.gachaconfig.repository.BannerPoolItemDao;
import com.example.gacha.item.dto.ItemCreateRequest;
import com.example.gacha.item.dto.ItemPatchRequest;
import com.example.gacha.item.dto.ItemResponse;
import com.example.gacha.item.dto.ItemSearchRequest;
import com.example.gacha.item.dto.ItemUpdateRequest;
import com.example.gacha.item.entity.Item;
import com.example.gacha.item.mapper.ItemMapper;
import com.example.gacha.item.repository.ItemDao;
import com.example.gacha.item.repository.ItemSpecifications;

/**
 * Item catalogue.
 * <ul>
 * <li>{@code code} is unique and immutable.</li>
 * <li>While an item is in the pool of a started banner, its rarity cannot change and it cannot
 * be deactivated - either would silently change odds players have already pulled under.</li>
 * <li>An item used in any pool cannot be deleted; deactivate it instead.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ItemService {

    private static final String ENTITY = "Item";

    private static final Set<String> SORTABLE_FIELDS =
            PageableSupport.sortableFields("code", "name", "itemType", "rarity", "active");

    private final ItemDao itemDao;
    private final BannerPoolItemDao poolDao;

    public ItemService(ItemDao itemDao, BannerPoolItemDao poolDao) {
        this.itemDao = itemDao;
        this.poolDao = poolDao;
    }

    public PageResponse<ItemResponse> search(ItemSearchRequest request, Pageable pageable) {
        Page<Item> page = itemDao.findAll(ItemSpecifications.from(request),
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, ItemMapper::toResponse);
    }

    public ItemResponse getById(Long id) {
        return ItemMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        Guards.assertNotTaken(itemDao.existsByCodeIgnoreCase(request.code()), "code", "an item", request.code());
        return ItemMapper.toResponse(itemDao.save(ItemMapper.toEntity(request)));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        Item item = findOrThrow(id);
        Integer rarity = item.getRarity();
        Boolean active = item.getActive();
        ItemMapper.replace(item, request);
        assertLiveUsageUntouched(item, rarity, active);
        return ItemMapper.toResponse(item);
    }

    @Transactional
    public ItemResponse patch(Long id, ItemPatchRequest request) {
        Item item = findOrThrow(id);
        Integer rarity = item.getRarity();
        Boolean active = item.getActive();
        ItemMapper.merge(item, request);
        assertLiveUsageUntouched(item, rarity, active);
        return ItemMapper.toResponse(item);
    }

    @Transactional
    public void delete(Long id) {
        Item item = findOrThrow(id);
        if (poolDao.existsByItemId(id)) {
            throw new IllegalArgumentException("id: the item is used in a banner pool, deactivate it instead");
        }
        itemDao.delete(item);
    }

    private Item findOrThrow(Long id) {
        return Entities.findOrThrow(itemDao, id, ENTITY);
    }

    private void assertLiveUsageUntouched(Item item, Integer oldRarity, Boolean oldActive) {
        boolean rarityChanged = !Objects.equals(oldRarity, item.getRarity());
        boolean deactivated = Boolean.TRUE.equals(oldActive) && !Boolean.TRUE.equals(item.getActive());
        if ((rarityChanged || deactivated) && poolDao.existsInStartedBanner(item.getId(), LocalDateTime.now())) {
            throw new IllegalArgumentException(rarityChanged
                    ? "rarity: the item is in the pool of a live banner, its rarity can no longer change"
                    : "active: the item is in the pool of a live banner, it cannot be deactivated");
        }
    }
}
