package com.swifteats.order.repo;

import com.swifteats.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByClientOrderId(String clientOrderId);
}
