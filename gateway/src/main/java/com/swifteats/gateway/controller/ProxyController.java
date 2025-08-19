package com.swifteats.gateway.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/restaurants")
public class ProxyController {

    private final RestTemplate rest;

    // Default constructor used in runtime
    public ProxyController() {
        this.rest = new RestTemplate();
    }

    // Constructor for tests / injection
    public ProxyController(RestTemplate rest) {
        this.rest = rest == null ? new RestTemplate() : rest;
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<String> proxyGetMenu(@PathVariable("id") Long id) {
        String url = String.format("http://catalog-service:8081/restaurants/%d/menu", id);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        HttpHeaders headers = new HttpHeaders();
        resp.getHeaders().forEach((k, v) -> {
            if (!"transfer-encoding".equalsIgnoreCase(k) && !"content-length".equalsIgnoreCase(k)) {
                headers.put(k, v);
            }
        });
        return new ResponseEntity<>(resp.getBody(), headers, resp.getStatusCode());
    }
}
