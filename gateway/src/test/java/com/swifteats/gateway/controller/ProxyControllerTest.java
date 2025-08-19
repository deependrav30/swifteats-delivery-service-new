package com.swifteats.gateway.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyControllerTest {

    @Test
    void proxyGetMenu_forwardsRequestAndReturnsBody() {
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        String menuJson = "[{\"id\":1,\"name\":\"Masala Dosa\",\"priceCents\":12000}]";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        ResponseEntity<String> remote = new ResponseEntity<>(menuJson, headers, HttpStatus.OK);

        Mockito.when(mockRest.exchange(Mockito.anyString(), Mockito.eq(org.springframework.http.HttpMethod.GET), Mockito.eq(org.springframework.http.HttpEntity.EMPTY), Mockito.eq(String.class)))
                .thenReturn(remote);

        ProxyController controller = new ProxyController(mockRest);
        ResponseEntity<String> resp = controller.proxyGetMenu(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(menuJson);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("application/json");
    }
}
