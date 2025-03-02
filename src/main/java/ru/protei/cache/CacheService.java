package ru.protei.cache;

import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CacheService {
    private static final Logger logger = Logger.getLogger(CacheService.class.getName());
    private final RedissonClient redissonClient;

    public CacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void put(int key, String value, long secondsToLive) {
        RMapCache<Integer, String> mapCache = redissonClient.getMapCache("cache");
        mapCache.put(key, value, secondsToLive, TimeUnit.SECONDS);
    }

    public String get(int key) {
        RMapCache<Integer, String> mapCache = redissonClient.getMapCache("cache");
        return mapCache.get(key);
    }
}
