package io.github.HenriqueMichelini.craftalism.economy.infra.api.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BalanceCacheRepository {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final long TTL_MINUTES = 30;

    private final Cache<UUID, Balance> cache;

    public BalanceCacheRepository() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public BalanceCacheRepository(int maxSize, long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public Optional<Balance> find(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    public void save(Balance balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Balance cannot be null");
        }
        if (balance.getUuid() == null) {
            throw new IllegalArgumentException("Balance UUID cannot be null");
        }

        Balance copy = new Balance(balance.getUuid(), balance.getAmount());
        cache.put(balance.getUuid(), copy);
    }

    public void delete(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        cache.invalidate(uuid);
    }

    public void clear() {
        cache.invalidateAll();
        cache.cleanUp();
    }

    public long size() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    public boolean contains(UUID uuid) {
        return find(uuid).isPresent();
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public String getStatsFormatted() {
        CacheStats stats = cache.stats();

        long requests = stats.requestCount();
        long hits = stats.hitCount();
        long misses = stats.missCount();
        double hitRate = requests > 0 ? (double) hits / requests * 100 : 0.0;

        return String.format(
                "Cache Stats - Size: %d/%d | Requests: %d | Hits: %d (%.2f%%) | Misses: %d | Evictions: %d | Load Success: %d | Load Failures: %d",
                size(),
                MAX_CACHE_SIZE,
                requests,
                hits,
                hitRate,
                misses,
                stats.evictionCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount()
        );
    }

    public void cleanUp() {
        cache.cleanUp();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void refresh(UUID uuid) {
        delete(uuid);
    }
}