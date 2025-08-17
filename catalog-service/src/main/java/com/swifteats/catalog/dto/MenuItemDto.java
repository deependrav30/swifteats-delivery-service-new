package com.swifteats.catalog.dto;

public class MenuItemDto {
    private Long id;
    private String name;
    private String description;
    private Long priceCents;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }
}
