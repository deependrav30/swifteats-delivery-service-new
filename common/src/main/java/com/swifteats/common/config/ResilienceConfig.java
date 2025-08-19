package com.swifteats.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ResilienceConfig {
    // intentionally minimal: Resilience4j will be configured via application.yml properties and annotations

    private static final Random RANDOM = new Random();

    private static IntervalFunction withJitter(IntervalFunction base, double jitterFraction) {
        return attempts -> {
            long baseMillis = base.apply(attempts);
            double jitterFactor = 1.0 - jitterFraction + (RANDOM.nextDouble() * (2 * jitterFraction));
            return Math.max(1L, (long) (baseMillis * jitterFactor));
        };
    }

    @Bean
    public RetryRegistry retryRegistry(MeterRegistry meterRegistry) {
        // exponential backoff: base 100ms, multiplier 2x + 25% jitter
        IntervalFunction base = IntervalFunction.ofExponentialBackoff(100L, 2.0);
        IntervalFunction jittered = withJitter(base, 0.25);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .intervalFunction(jittered)
                .retryExceptions(RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        // Return registry; metrics binder bean will expose metrics
        return registry;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(20)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cbConfig);
        return registry;
    }

    @Bean
    public Object resilienceMetricsBinder(MeterRegistry meterRegistry, CircuitBreakerRegistry cbRegistry, RetryRegistry retryRegistry) {
        // basic binder: expose circuit breaker state and failure rate, and retry counts
        Map<String, Counter> retryCounters = new ConcurrentHashMap<>();

        // bind existing CBs
        cbRegistry.getAllCircuitBreakers().forEach(cb -> bindCircuitBreaker(cb, meterRegistry));

        // listen for future CB creations
        cbRegistry.getEventPublisher().onEntryAdded(event -> bindCircuitBreaker(event.getAddedEntry(), meterRegistry));

        // bind retries: create counters per retry name
        retryRegistry.getAllRetries().forEach(r -> retryCounters.putIfAbsent(r.getName(), Counter.builder("resilience_retry_count").tag("name", r.getName()).register(meterRegistry)));
        retryRegistry.getEventPublisher().onEntryAdded(event -> retryCounters.putIfAbsent(event.getAddedEntry().getName(), Counter.builder("resilience_retry_count").tag("name", event.getAddedEntry().getName()).register(meterRegistry)));

        return new Object();
    }

    private void bindCircuitBreaker(CircuitBreaker cb, MeterRegistry meterRegistry) {
        String name = cb.getName();
        // gauge for state: 0 CLOSED, 1 OPEN, 2 HALF_OPEN
        Gauge.builder("resilience_circuitbreaker_state", cb, c -> {
            CircuitBreaker.State s = c.getState();
            if (s == CircuitBreaker.State.CLOSED) return 0;
            if (s == CircuitBreaker.State.OPEN) return 1;
            return 2;
        }).tag("name", name).register(meterRegistry);

        // gauge for failure rate
        Gauge.builder("resilience_circuitbreaker_failure_rate", cb, c -> c.getMetrics().getFailureRate()).tag("name", name).register(meterRegistry);

        // gauge for current number of buffered calls
        Gauge.builder("resilience_circuitbreaker_buffered_calls", cb, c -> (double) c.getMetrics().getNumberOfBufferedCalls()).tag("name", name).register(meterRegistry);
    }
}
