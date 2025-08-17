package com.swifteats.order.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clientOrderId;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private Long totalCents;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public Long getTotalCents() { return totalCents; }
    public void setTotalCents(Long totalCents) { this.totalCents = totalCents; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
