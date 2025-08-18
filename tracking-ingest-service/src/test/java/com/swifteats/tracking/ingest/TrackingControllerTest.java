package com.swifteats.tracking.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.tracking.ingest.dto.DriverLocationDto;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.swifteats.tracking.ingest.controller.TrackingController.class)
public class TrackingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AmqpTemplate amqpTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testPushLocationSuccess() throws Exception {
        DriverLocationDto dto = new DriverLocationDto("driver-1", 18.5204, 73.8567, 12.3, Instant.now());
        doNothing().when(amqpTemplate).convertAndSend(eq("driver.location.v1"), eq("driver.driver-1"), any(DriverLocationDto.class));

        mvc.perform(post("/tracking/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testPushLocationFailure() throws Exception {
        DriverLocationDto dto = new DriverLocationDto("driver-1", 18.5204, 73.8567, 12.3, Instant.now());
        // if Rabbit publish fails, controller writes to Redis and still returns 202 Accepted
        doThrow(new RuntimeException("amqp down")).when(amqpTemplate).convertAndSend(eq("driver.location.v1"), eq("driver.driver-1"), any(DriverLocationDto.class));

        mvc.perform(post("/tracking/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());
    }
}
