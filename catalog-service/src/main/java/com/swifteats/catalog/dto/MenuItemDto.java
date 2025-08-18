package com.swifteats.catalog.dto;

public class MenuItemDto {
    private Long id;
    private String name;
    private Integer priceCents;

    public MenuItemDto() {}

    public MenuItemDto(Long id, String name, Integer priceCents) {
        this.id = id;
        this.name = name;
        this.priceCents = priceCents;
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

    public Integer getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(Integer priceCents) {
        this.priceCents = priceCents;
    }
}
