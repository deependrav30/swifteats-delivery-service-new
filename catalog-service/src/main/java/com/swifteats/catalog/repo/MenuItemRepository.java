package com.swifteats.catalog.repo;

import com.swifteats.catalog.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdOrderByIdAsc(Long restaurantId);
    void deleteByRestaurantId(Long restaurantId);
}
