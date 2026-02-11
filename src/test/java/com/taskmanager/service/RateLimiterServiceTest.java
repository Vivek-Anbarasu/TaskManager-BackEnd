package com.taskmanager.service;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "capacity", 10L);
        ReflectionTestUtils.setField(rateLimiterService, "refillTokens", 1L);
        ReflectionTestUtils.setField(rateLimiterService, "refillDurationSeconds", 10L);
        ReflectionTestUtils.setField(rateLimiterService, "greedyRefill", true);
        ReflectionTestUtils.setField(rateLimiterService, "tokensPerRequest", 1L);
    }

    @Test
    void tryConsumeReturnsTrueWhenTokensAreAvailable() {
        boolean result = rateLimiterService.tryConsume("user@example.com");

        assertTrue(result);
    }

    @Test
    void tryConsumeReturnsFalseWhenTokensAreExhausted() {
        String key = "user@example.com";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(key);
        }

        boolean result = rateLimiterService.tryConsume(key);

        assertFalse(result);
    }

    @Test
    void tryConsumeDecreasesAvailableTokens() {
        String key = "user@example.com";
        long initialTokens = rateLimiterService.getAvailableTokens(key);

        rateLimiterService.tryConsume(key);

        long tokensAfter = rateLimiterService.getAvailableTokens(key);
        assertEquals(initialTokens - 1, tokensAfter);
    }

    @Test
    void tryConsumeCreatesNewBucketForNewKey() {
        String key1 = "user1@example.com";
        String key2 = "user2@example.com";

        rateLimiterService.tryConsume(key1);
        rateLimiterService.tryConsume(key2);

        long tokens1 = rateLimiterService.getAvailableTokens(key1);
        long tokens2 = rateLimiterService.getAvailableTokens(key2);

        assertEquals(9L, tokens1);
        assertEquals(9L, tokens2);
    }

    @Test
    void tryConsumeReusesBucketForSameKey() {
        String key = "user@example.com";

        rateLimiterService.tryConsume(key);
        rateLimiterService.tryConsume(key);

        long tokens = rateLimiterService.getAvailableTokens(key);

        assertEquals(8L, tokens);
    }

    @Test
    void tryConsumeConsumesCorrectNumberOfTokens() {
        ReflectionTestUtils.setField(rateLimiterService, "tokensPerRequest", 3L);
        String key = "user@example.com";

        rateLimiterService.tryConsume(key);

        long tokens = rateLimiterService.getAvailableTokens(key);
        assertEquals(7L, tokens);
    }

    @Test
    void tryConsumeReturnsFalseWhenNotEnoughTokensAvailable() {
        ReflectionTestUtils.setField(rateLimiterService, "tokensPerRequest", 5L);
        String key = "user@example.com";

        rateLimiterService.tryConsume(key);
        rateLimiterService.tryConsume(key);

        boolean result = rateLimiterService.tryConsume(key);

        assertFalse(result);
    }


    @Test
    void tryConsumeHandlesEmptyKey() {
        boolean result = rateLimiterService.tryConsume("");

        assertTrue(result);
    }

    @Test
    void tryConsumeHandlesMultipleConcurrentKeys() {
        String key1 = "user1@example.com";
        String key2 = "user2@example.com";
        String key3 = "user3@example.com";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(key1);
        }

        boolean result1 = rateLimiterService.tryConsume(key1);
        boolean result2 = rateLimiterService.tryConsume(key2);
        boolean result3 = rateLimiterService.tryConsume(key3);

        assertFalse(result1);
        assertTrue(result2);
        assertTrue(result3);
    }

    @Test
    void resolveBucketReturnsNonNullBucket() {
        Bucket bucket = rateLimiterService.resolveBucket("user@example.com");

        assertNotNull(bucket);
    }

    @Test
    void resolveBucketReturnsSameBucketForSameKey() {
        String key = "user@example.com";

        Bucket bucket1 = rateLimiterService.resolveBucket(key);
        Bucket bucket2 = rateLimiterService.resolveBucket(key);

        assertSame(bucket1, bucket2);
    }

    @Test
    void resolveBucketReturnsDifferentBucketsForDifferentKeys() {
        Bucket bucket1 = rateLimiterService.resolveBucket("user1@example.com");
        Bucket bucket2 = rateLimiterService.resolveBucket("user2@example.com");

        assertNotSame(bucket1, bucket2);
    }

    @Test
    void getAvailableTokensReturnsCapacityForNewBucket() {
        String key = "user@example.com";

        long tokens = rateLimiterService.getAvailableTokens(key);

        assertEquals(10L, tokens);
    }

    @Test
    void getAvailableTokensReturnsCorrectValueAfterConsumption() {
        String key = "user@example.com";

        rateLimiterService.tryConsume(key);
        rateLimiterService.tryConsume(key);
        rateLimiterService.tryConsume(key);

        long tokens = rateLimiterService.getAvailableTokens(key);

        assertEquals(7L, tokens);
    }

    @Test
    void getAvailableTokensReturnsZeroWhenAllTokensConsumed() {
        String key = "user@example.com";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(key);
        }

        long tokens = rateLimiterService.getAvailableTokens(key);

        assertEquals(0L, tokens);
    }

    @Test
    void clearBucketRemovesBucketForKey() {
        String key = "user@example.com";

        rateLimiterService.tryConsume(key);
        rateLimiterService.clearBucket(key);

        long tokens = rateLimiterService.getAvailableTokens(key);

        assertEquals(10L, tokens);
    }

    @Test
    void clearBucketDoesNotAffectOtherBuckets() {
        String key1 = "user1@example.com";
        String key2 = "user2@example.com";

        rateLimiterService.tryConsume(key1);
        rateLimiterService.tryConsume(key2);
        rateLimiterService.tryConsume(key2);

        rateLimiterService.clearBucket(key1);

        long tokens1 = rateLimiterService.getAvailableTokens(key1);
        long tokens2 = rateLimiterService.getAvailableTokens(key2);

        assertEquals(10L, tokens1);
        assertEquals(8L, tokens2);
    }

    @Test
    void clearBucketHandlesNonExistentKey() {
        assertDoesNotThrow(() -> rateLimiterService.clearBucket("nonexistent@example.com"));
    }

    @Test
    void clearAllBucketsRemovesAllBuckets() {
        String key1 = "user1@example.com";
        String key2 = "user2@example.com";

        rateLimiterService.tryConsume(key1);
        rateLimiterService.tryConsume(key1);
        rateLimiterService.tryConsume(key2);

        rateLimiterService.clearAllBuckets();

        long tokens1 = rateLimiterService.getAvailableTokens(key1);
        long tokens2 = rateLimiterService.getAvailableTokens(key2);

        assertEquals(10L, tokens1);
        assertEquals(10L, tokens2);
    }

    @Test
    void clearAllBucketsHandlesEmptyCache() {
        assertDoesNotThrow(() -> rateLimiterService.clearAllBuckets());
    }

    @Test
    void bucketUsesGreedyRefillWhenConfigured() {
        ReflectionTestUtils.setField(rateLimiterService, "greedyRefill", true);

        Bucket bucket = rateLimiterService.resolveBucket("user@example.com");

        assertNotNull(bucket);
        assertEquals(10L, bucket.getAvailableTokens());
    }

    @Test
    void bucketUsesIntervallyRefillWhenConfigured() {
        ReflectionTestUtils.setField(rateLimiterService, "greedyRefill", false);

        Bucket bucket = rateLimiterService.resolveBucket("user@example.com");

        assertNotNull(bucket);
        assertEquals(10L, bucket.getAvailableTokens());
    }

    @Test
    void bucketRespectsConfiguredCapacity() {
        ReflectionTestUtils.setField(rateLimiterService, "capacity", 100L);

        long tokens = rateLimiterService.getAvailableTokens("user@example.com");

        assertEquals(100L, tokens);
    }

    @Test
    void tryConsumeHandlesLargeCapacity() {
        ReflectionTestUtils.setField(rateLimiterService, "capacity", 1000L);
        String key = "user@example.com";

        for (int i = 0; i < 999; i++) {
            assertTrue(rateLimiterService.tryConsume(key));
        }

        assertTrue(rateLimiterService.tryConsume(key));
        assertFalse(rateLimiterService.tryConsume(key));
    }

    @Test
    void tryConsumeHandlesMinimalCapacity() {
        ReflectionTestUtils.setField(rateLimiterService, "capacity", 1L);
        String key = "user@example.com";

        assertTrue(rateLimiterService.tryConsume(key));
        assertFalse(rateLimiterService.tryConsume(key));
    }

    @Test
    void multipleKeysDoNotShareTokens() {
        String key1 = "user1@example.com";
        String key2 = "user2@example.com";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(key1);
        }

        boolean result = rateLimiterService.tryConsume(key2);

        assertTrue(result);
        assertEquals(0L, rateLimiterService.getAvailableTokens(key1));
        assertEquals(9L, rateLimiterService.getAvailableTokens(key2));
    }

    @Test
    void tryConsumeWithSpecialCharactersInKey() {
        String key = "user+test@example.co.uk";

        boolean result = rateLimiterService.tryConsume(key);

        assertTrue(result);
        assertEquals(9L, rateLimiterService.getAvailableTokens(key));
    }

    @Test
    void tryConsumeWithNumericKey() {
        String key = "12345";

        boolean result = rateLimiterService.tryConsume(key);

        assertTrue(result);
        assertEquals(9L, rateLimiterService.getAvailableTokens(key));
    }

    @Test
    void clearBucketAllowsNewBucketCreationWithFullCapacity() {
        String key = "user@example.com";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(key);
        }

        rateLimiterService.clearBucket(key);

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.tryConsume(key));
        }

        assertFalse(rateLimiterService.tryConsume(key));
    }
}

