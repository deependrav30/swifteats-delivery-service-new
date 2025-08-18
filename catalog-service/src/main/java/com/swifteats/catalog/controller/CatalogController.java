package com.swifteats.catalog.controller;

import com.swifteats.catalog.dto.MenuItemDto;
import com.swifteats.catalog.dto.RestaurantDto;
import com.swifteats.catalog.service.CatalogService;
import com.swifteats.catalog.config.CacheConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * GET /restaurants/{id}/menu uses cache name: CacheConfig.RESTAURANT_MENU_CACHE
 * Redis key pattern (Spring Cache default): "{cacheName}::{key}"
 * For this endpoint the effective Redis key will be: "{CacheConfig.RESTAURANT_MENU_CACHE}::{restaurantId}"
 * TTL is configured in CacheConfig (restaurantMenu TTL = 60 seconds).
 */
@RestController
@RequestMapping("/restaurants")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<MenuItemDto>> getMenu(@PathVariable("id") Long id) {
        return ResponseEntity.ok(catalogService.getMenu(id));
    }

    @PostMapping
    public ResponseEntity<RestaurantDto> createRestaurant(@RequestBody RestaurantDto dto) {
        RestaurantDto created = catalogService.createRestaurant(dto);
        return ResponseEntity.created(URI.create("/restaurants/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/menu")
    public ResponseEntity<List<MenuItemDto>> replaceMenu(@PathVariable("id") Long id, @RequestBody List<MenuItemDto> menu) {
        List<MenuItemDto> updated = catalogService.replaceMenu(id, menu);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/menu")
    public ResponseEntity<MenuItemDto> addMenuItem(@PathVariable("id") Long id, @RequestBody MenuItemDto item) {
        MenuItemDto saved = catalogService.addMenuItem(id, item);
        return ResponseEntity.ok(saved);
    }
}
