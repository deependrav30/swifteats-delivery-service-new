package com.swifteats.gateway.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/orders")
public class OrdersProxyController {

    private final RestTemplate rest = new RestTemplate();

    @PostMapping
    public ResponseEntity<byte[]> postOrder(HttpServletRequest request, @RequestBody(required = false) String body) {
        String url = "http://order-service:8082/orders";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // forward dev auth header or Authorization if present
        String devJwt = request.getHeader("X-DEV-JWT");
        if (devJwt != null && !devJwt.isBlank()) {
            headers.set("X-DEV-JWT", devJwt);
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            headers.set("Authorization", auth);
        }
        HttpEntity<String> entity = new HttpEntity<>(body == null ? "" : body, headers);
        ResponseEntity<byte[]> resp = rest.exchange(url, HttpMethod.POST, entity, byte[].class);
        HttpHeaders out = new HttpHeaders();
        resp.getHeaders().forEach((k, v) -> {
            if (!"transfer-encoding".equalsIgnoreCase(k) && !"content-length".equalsIgnoreCase(k) && !"connection".equalsIgnoreCase(k)) {
                out.put(k, v);
            }
        });
        return new ResponseEntity<>(resp.getBody(), out, resp.getStatusCode());
    }

    @PostMapping("/{path:^(?:.*)$}")
    public ResponseEntity<byte[]> postOrderPath(HttpServletRequest request, @PathVariable("path") String path, @RequestBody(required = false) String body) {
        String url = String.format("http://order-service:8082/orders/%s", path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // forward dev auth header or Authorization if present
        String devJwt = request.getHeader("X-DEV-JWT");
        if (devJwt != null && !devJwt.isBlank()) {
            headers.set("X-DEV-JWT", devJwt);
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            headers.set("Authorization", auth);
        }
        HttpEntity<String> entity = new HttpEntity<>(body == null ? "" : body, headers);
        ResponseEntity<byte[]> resp = rest.exchange(url, HttpMethod.POST, entity, byte[].class);
        HttpHeaders out = new HttpHeaders();
        resp.getHeaders().forEach((k, v) -> {
            if (!"transfer-encoding".equalsIgnoreCase(k) && !"content-length".equalsIgnoreCase(k) && !"connection".equalsIgnoreCase(k)) {
                out.put(k, v);
            }
        });
        return new ResponseEntity<>(resp.getBody(), out, resp.getStatusCode());
    }
}
