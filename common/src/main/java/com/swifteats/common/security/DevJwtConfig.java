package com.swifteats.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "security", name = "dev-jwt-enabled", havingValue = "true", matchIfMissing = false)
public class DevJwtConfig {

    private static final Logger log = LoggerFactory.getLogger(DevJwtConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("DevJwtConfig.securityFilterChain() - dev-jwt enabled, registering security filter chain");
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // allow demo POSTs to /orders without failing auth (dev-only)
            .requestMatchers("/orders", "/orders/**").permitAll()
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults());

        // Add a simple JWT-like header parser filter to populate SecurityContext
        http.addFilterBefore(devJwtFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public OncePerRequestFilter devJwtFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                String token = request.getHeader("X-DEV-JWT");
                if (token != null && !token.isBlank()) {
                    log.info("DevJwtFilter found X-DEV-JWT header: {}", token);
                    // token format: role:username (e.g., ROLE_ADMIN:dev)
                    String[] parts = token.split(":", 2);
                    String role = parts.length > 0 ? parts[0] : "ROLE_USER";
                    String username = parts.length > 1 ? parts[1] : "dev";
                    log.info("DevJwtFilter parsed role='{}' username='{}'", role, username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
