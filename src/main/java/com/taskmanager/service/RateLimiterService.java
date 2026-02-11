package com.taskmanager.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimiterService {

    @Value("${rate.limit.capacity}")
    private long capacity;

    @Value("${rate.limit.refill.tokens}")
    private long refillTokens;

    @Value("${rate.limit.refill.duration-seconds:60}")
    private long refillDurationSeconds;

    @Value("${rate.limit.refill.greedy:false}")
    private boolean greedyRefill;

    @Value("${rate.limit.tokens-per-request}")
    private long tokensPerRequest;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit;
        if (greedyRefill) {
            // Greedy refill: tokens are added continuously at a steady rate
            limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(refillTokens, Duration.ofSeconds(refillDurationSeconds))
                    .build();
        } else {
            // Intervally refill: tokens are added in batches at fixed intervals
            limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillIntervally(refillTokens, Duration.ofSeconds(refillDurationSeconds))
                    .build();
        }
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        boolean consumed = bucket.tryConsume(tokensPerRequest);
        if (!consumed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        return consumed;
    }

    public long getAvailableTokens(String key) {
        return resolveBucket(key).getAvailableTokens();
    }

    public void clearBucket(String key) {
        cache.remove(key);
        log.debug("Cleared rate limit bucket for key: {}", key);
    }

    public void clearAllBuckets() {
        cache.clear();
        log.info("Cleared all rate limit buckets");
    }
}

