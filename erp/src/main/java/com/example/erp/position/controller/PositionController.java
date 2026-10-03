package com.example.erp.position.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.common.constant.ApiPaths;
import com.example.erp.common.constant.ErpConstants;
import com.example.erp.position.entity.Position;
import com.example.erp.position.service.PositionService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Position CRUD API. Every method answers with the {@link ApiResponse} envelope
 * and
 * {@link PositionResponse} transfer objects; entities never leave the service
 * layer.
 */
@RestController
@RequestMapping("/api/positions")
public class PositionController {
    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping("")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }
}
