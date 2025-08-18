package com.swifteats.catalog.service;

import com.swifteats.catalog.dto.MenuItemDto;
import com.swifteats.catalog.dto.RestaurantDto;
import com.swifteats.catalog.model.MenuItem;
import com.swifteats.catalog.model.Restaurant;
import com.swifteats.catalog.repo.MenuItemRepository;
import com.swifteats.catalog.repo.RestaurantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    public static final String MENU_CACHE = "catalog.restaurant.menu";

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public CatalogService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Cacheable(value = MENU_CACHE, key = "#p0")
    public List<MenuItemDto> getMenu(Long restaurantId) {
        List<MenuItem> items = menuItemRepository.findByRestaurantIdOrderByIdAsc(restaurantId);
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public RestaurantDto createRestaurant(RestaurantDto dto) {
        Restaurant r = new Restaurant(dto.getName());
        if (dto.getMenu() != null) {
            dto.getMenu().forEach(mi -> r.addMenuItem(new MenuItem(mi.getName(), mi.getPriceCents())));
        }
        Restaurant saved = restaurantRepository.save(r);
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = MENU_CACHE, key = "#p0")
    public List<MenuItemDto> replaceMenu(Long restaurantId, List<MenuItemDto> newMenu) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        // clear existing
        restaurant.clearMenu();
        // add new
        newMenu.forEach(mi -> restaurant.addMenuItem(new MenuItem(mi.getName(), mi.getPriceCents())));
        restaurantRepository.save(restaurant);
        return restaurant.getMenuItems().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = MENU_CACHE, key = "#p0")
    public MenuItemDto addMenuItem(Long restaurantId, MenuItemDto itemDto) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        MenuItem item = new MenuItem(itemDto.getName(), itemDto.getPriceCents());
        restaurant.addMenuItem(item);
        Restaurant saved = restaurantRepository.save(restaurant);
        // find the added item (last)
        MenuItem savedItem = saved.getMenuItems().get(saved.getMenuItems().size() - 1);
        return toDto(savedItem);
    }

    private MenuItemDto toDto(MenuItem m) {
        return new MenuItemDto(m.getId(), m.getName(), m.getPriceCents());
    }

    private RestaurantDto toDto(Restaurant r) {
        List<MenuItemDto> menu = r.getMenuItems().stream().map(this::toDto).collect(Collectors.toList());
        return new RestaurantDto(r.getId(), r.getName(), menu);
    }
}
