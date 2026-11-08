package com.example.gacha.item.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.gacha.common.constant.ApiPaths;
import com.example.gacha.common.constant.Constants;
import com.example.gacha.item.dto.ItemCreateRequest;
import com.example.gacha.item.dto.ItemPatchRequest;
import com.example.gacha.item.dto.ItemResponse;
import com.example.gacha.item.dto.ItemSearchRequest;
import com.example.gacha.item.dto.ItemUpdateRequest;
import com.example.gacha.item.service.ItemService;

import jakarta.validation.Valid;

/** Item catalogue API. */
@RestController
@RequestMapping(ApiPaths.ITEMS)
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * GET /api/items?keyword=blade&amp;itemType=WEAPON&amp;rarity=5&amp;active=true
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            @ModelAttribute ItemSearchRequest request,
            @PageableDefault(size = Constants.DEFAULT_PAGE_SIZE, sort = Constants.ID, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(itemService.search(request, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(itemService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> replaceItem(
            @PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(itemService.update(id, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> patchItem(
            @PathVariable Long id, @Valid @RequestBody ItemPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(itemService.patch(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
