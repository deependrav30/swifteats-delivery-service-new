package com.swifteats.order.poller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final AmqpTemplate amqpTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, AmqpTemplate amqpTemplate) {
        this.outboxRepository = outboxRepository;
        this.amqpTemplate = amqpTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll.interval.ms:5000}")
    @Timed("outbox.poll.duration")
    @Retry(name = "outboxRetry")
    @CircuitBreaker(name = "outboxCircuit")
    @Transactional
    public void pollAndPublish() throws JsonProcessingException {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent e : events) {
            try {
                amqpTemplate.convertAndSend("swifteats.order.events", e.getEventType(), e.getPayload());

                // additionally, when an order is created, forward a payment request to payments.incoming queue
                if ("order.created".equals(e.getEventType())) {
                    amqpTemplate.convertAndSend("", "payments.incoming", e.getPayload());
                    log.info("Forwarded order {} payload to payments.incoming", e.getAggregateId());
                }

                e.setPublished(true);
                e.setStatus("SENT");
                e.setLastAttemptAt(Instant.now());
                outboxRepository.save(e);
                log.info("Published outbox id={} eventType={}", e.getId(), e.getEventType());
            } catch (Exception ex) {
                e.setAttempts(e.getAttempts() + 1);
                e.setLastAttemptAt(Instant.now());
                if (e.getAttempts() >= 5) {
                    e.setStatus("FAILED");
                    log.error("Outbox id={} failed after {} attempts", e.getId(), e.getAttempts(), ex);
                } else {
                    // leave status as PENDING for retry
                    log.warn("Outbox id={} publish attempt {} failed", e.getId(), e.getAttempts(), ex);
                }
                outboxRepository.save(e);
                // rethrow to allow resilience annotations to observe failures when appropriate
                throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
            }
        }
    }
}
