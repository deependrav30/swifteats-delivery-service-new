package com.swifteats.catalog.controller;

import com.swifteats.catalog.model.MenuItem;
import com.swifteats.catalog.repo.MenuItemRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class CatalogController {

    private final MenuItemRepository repo;

    public CatalogController(MenuItemRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{id}/menu")
    @Cacheable(value = "restaurantMenu", key = "#id")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable("id") Long id) {
        List<MenuItem> menu = repo.findByRestaurantId(id);
        return ResponseEntity.ok(menu);
    }

    @PostMapping("/{id}/menu")
    @CacheEvict(value = "restaurantMenu", key = "#id")
    public ResponseEntity<MenuItem> addMenuItem(@PathVariable("id") Long id, @RequestBody MenuItem item) {
        item.setRestaurantId(id);
        MenuItem saved = repo.save(item);
        return ResponseEntity.ok(saved);
    }
}
