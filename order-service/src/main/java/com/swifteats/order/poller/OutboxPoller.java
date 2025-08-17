package com.swifteats.order.poller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final AmqpTemplate amqpTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, AmqpTemplate amqpTemplate) {
        this.outboxRepository = outboxRepository;
        this.amqpTemplate = amqpTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll.interval.ms:5000}")
    @Transactional
    public void pollAndPublish() throws JsonProcessingException {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent e : events) {
            try {
                amqpTemplate.convertAndSend("swifteats.order.events", e.getEventType(), e.getPayload());
                e.setPublished(true);
                outboxRepository.save(e);
            } catch (Exception ex) {
                // log and continue; published flag remains false
            }
        }
    }
}
