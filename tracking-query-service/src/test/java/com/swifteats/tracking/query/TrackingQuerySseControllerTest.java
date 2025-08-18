package com.swifteats.tracking.query;

import com.swifteats.tracking.query.controller.TrackingQuerySseController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrackingQuerySseController.class)
public class TrackingQuerySseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOps;

    @Test
    void streamEndpointConnectsSuccessfully() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        mockMvc.perform(get("/drivers/driver-1/location/stream")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }
}
