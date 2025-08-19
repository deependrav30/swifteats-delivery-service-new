package com.swifteats.order.poller;

import com.swifteats.order.model.OutboxEvent;
import com.swifteats.order.repo.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OutboxPollerResilienceTest {

    private OutboxRepository outboxRepository;
    private AmqpTemplate amqpTemplate;
    private OutboxPoller poller;

    @BeforeEach
    public void setup() {
        outboxRepository = mock(OutboxRepository.class);
        amqpTemplate = mock(AmqpTemplate.class);
        poller = new OutboxPoller(outboxRepository, amqpTemplate);
    }

    @Test
    public void whenPublishFails_thenRetriesAndUpdatesAttempts() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setEventType("order.created");
        event.setPayload("{}");
        event.setPublished(false);
        event.setAttempts(0);
        event.setCreatedAt(Instant.now());

        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(Arrays.asList(event));

        // Simulate AMQP send throwing exception first two times then succeeding
        doThrow(new RuntimeException("rabbit down")).doThrow(new RuntimeException("still down")).doNothing()
                .when(amqpTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Capture saved entity to verify attempts get incremented
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // call poller multiple times simulating scheduler retries
        for (int i = 0; i < 3; i++) {
            try {
                poller.pollAndPublish();
            } catch (Exception ex) {
                // expected on failed attempts
            }
        }

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, atLeast(1)).save(captor.capture());

        List<OutboxEvent> saved = captor.getAllValues();
        // last saved event should reflect attempts >=2
        OutboxEvent last = saved.get(saved.size() - 1);
        assertTrue(last.getAttempts() >= 2, "Attempts should be incremented after failures");
    }
}
