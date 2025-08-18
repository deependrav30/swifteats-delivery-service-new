package com.swifteats.catalog.dto;

import java.util.ArrayList;
import java.util.List;

public class RestaurantDto {
    private Long id;
    private String name;
    private List<MenuItemDto> menu = new ArrayList<>();

    public RestaurantDto() {}

    public RestaurantDto(Long id, String name, List<MenuItemDto> menu) {
        this.id = id;
        this.name = name;
        this.menu = menu;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MenuItemDto> getMenu() {
        return menu;
    }

    public void setMenu(List<MenuItemDto> menu) {
        this.menu = menu;
    }
}
