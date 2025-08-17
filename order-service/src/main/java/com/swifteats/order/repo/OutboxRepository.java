package com.swifteats.order.repo;

import com.swifteats.order.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
